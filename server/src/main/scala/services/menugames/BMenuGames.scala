package services.menugames

import errors.ErrorADT
import models.bff.outofgame.MenuGameWithPlayers
import menus.data.User

import zio.*
import java.time.LocalDateTime
import models.bff.outofgame.MenuGame
import models.bff.outofgame.gameconfig.*
import models.syntax.Pointed

import scala.concurrent.duration.Duration
import gamelogic.docs.BossMetadata
import models.bff.outofgame.gameconfig.GameConfiguration.GameConfigMetadata
import menus.data.GameCredentials
import models.bff.ingame.GameUserCredentials
import menus.data.AllGameCredentials
import services.localstorage.LocalStorage
import menus.data.GameCredentialsWithGameInfo

private[menugames] class BMenuGames(
    gamesRef: Ref[Vector[MenuGameWithPlayers]],
    gameUpdatesSemaphore: Semaphore,
    crypto: services.crypto.Crypto,
    storage: services.localstorage.LocalStorage,
    events: services.events.Events
) extends MenuGames {

  override def menuGames: ZIO[Any, Nothing, Vector[MenuGameWithPlayers]] = gamesRef.get

  override def launchGame(
      requester: User,
      gameId: String
  ): ZIO[Any, Nothing, Either[ErrorADT, Unit]] = updateGamesZIO(currentGames =>
    for {
      // todo: create all game credentials for all players
      storageKey      <- ZIO.succeed(gameCredentialsStoringKey(gameId))
      game            <- gameWithId(gameId, currentGames)
      alreadyLaunched <- ZIO.succeed(game.started)
      _               <- ZIO.when(alreadyLaunched)(ZIO.fail(ErrorADT.GameAlreadyLaunched(gameId)))
      _ <- ZIO
        .unless(game.isGameCreator(requester))(ZIO.fail(ErrorADT.YouAreNotCreator(requester.name)))
      secret          <- ZIO.succeed(java.util.UUID.randomUUID().toString)
      credsUrl        <- ZIO.succeed("http://localhost:9000/api/bff/internal/get-all-credentials")
      serverReadyUrl  <- ZIO.succeed("http://localhost:9000/api/bff/internal/game-server-ready")
      gameCredentials <- ZIO.succeed(GameCredentials(gameId, secret))
      userCredentials = game.players.map { user =>
        GameUserCredentials(user.name, gameId, java.util.UUID.randomUUID().toString)
      }
      allGameCredentials = AllGameCredentials(gameCredentials, userCredentials)
      _ <- storage
        .storeAtFor(
          storageKey,
          GameCredentialsWithGameInfo(allGameCredentials, game.start),
          Duration.Inf
        )
        .flatMapError(throwable =>
          ZIO.succeed(throwable.printStackTrace()) *> ZIO.succeed(
            ErrorADT
              .RawInternalError(throwable.getMessage)
          )
        )
      _ <- ZIO
        .attemptBlocking {
          requests.post(
            "http://localhost:22223/run-game-server",
            params = Map(
              "gameId"             -> game.id,
              "gameSecret"         -> secret,
              "credentialsUrl"     -> credsUrl,
              "gameServerReadyUrl" -> serverReadyUrl
            )
          )
        }
        .flatMapError(throwable =>
          ZIO.succeed(throwable.printStackTrace()) *> ZIO.succeed(
            ErrorADT.RawInternalError(throwable.getMessage)
          )
        )
      _ <- setAndStoreGames(patchGame(game.start, currentGames))
    } yield ()
  ).either

  override def retrieveAllGameCredentials(
      gameId: String,
      secret: String
  ): ZIO[Any, Nothing, Either[ErrorADT, GameCredentialsWithGameInfo]] = (for {
    gameInfo <- storage
      .retrieveFrom(gameCredentialsStoringKey(gameId))
      .catchAll(_ => ZIO.none)
      .someOrFail(ErrorADT.CouldNotFetchTokenFromGameServer)
    _ <- ZIO.unless(secret == gameInfo.secret)(ZIO.fail(ErrorADT.WrongGameCredentials))
  } yield gameInfo).either

  override def gameEnded(gameId: String, secret: String): ZIO[Any, Nothing, Unit] = for {
    gameInfo <- storage
      .retrieveFrom(gameCredentialsStoringKey(gameId))
      .catchAll(_ => ZIO.none)
    _ <- ZIO.when(gameInfo.exists(_.secret == secret))(deleteGame(gameId))
  } yield ()

  override def createGame(
      gameName: String,
      maybePassword: Option[String],
      gameCreator: User
  ): ZIO[Any, Nothing, Either[ErrorADT, MenuGameWithPlayers]] = updateGamesZIO(currentGames =>
    for {
      _ <- ZIO.when(currentGames.exists(_.game.gameName == gameName))(
        ZIO.fail(ErrorADT.GameExists(gameName))
      )
      _ <- ZIO.when(currentGames.exists(_.containsPlayer(gameCreator)))(
        ZIO.fail(ErrorADT.UserAlreadyPlaying(gameCreator.name))
      )
      now                 <- ZIO.succeed(LocalDateTime.now())
      maybeHashedPassword <- maybePassword.map(crypto.hashPassword(_).asSome).getOrElse(ZIO.none)
      gameId              <- ZIO.succeed(java.util.UUID.randomUUID().toString)
      menuGame = MenuGame(
        gameId,
        gameName,
        maybeHashedPassword.map(_.pw),
        gameCreator,
        now,
        GameConfiguration(
          Map(
            gameCreator.name -> PlayerInfo(
              PlayerName.HumanPlayerName(gameCreator.name),
              None,
              None,
              PlayerStatus.NotReady,
              PlayerType.Human
            )
          ),
          BossMetadata.firstBossName
        )
      )
      maybeValidationErrors <- MenuGame.validator.validateZIO(menuGame)
      _ <- maybeValidationErrors
        .map(errors => ZIO.fail(ErrorADT.MultipleErrorsMap(errors)))
        .getOrElse(ZIO.unit)
      menuGameWithPlayers = MenuGameWithPlayers(menuGame, Vector(gameCreator), false)
      _ <- updateAndStoreGames(_ :+ menuGameWithPlayers)
    } yield menuGameWithPlayers
  ).either

  override def deleteGame(gameId: String) =
    updateGamesZIO(_ => updateAndStoreGames(_.filterNot(_.id == gameId)).unit)

  override def joinGame(user: User, gameId: String, maybePassword: Option[String]) =
    updateGamesZIO(currentGames =>
      for {
        _    <- playerIsAlreadyPlayingCheck(currentGames, user)
        game <- gameWithId(gameId, currentGames)
        hashedPassword <- maybePassword
          .map(pw => crypto.hashPassword(pw).asSome)
          .getOrElse(ZIO.none)
        _ <- ZIO.when(
          game.game.maybeHashedPassword.fold(false)(pw =>
            !hashedPassword.map(_.pw).contains[String](pw)
          )
        )(ZIO.fail(ErrorADT.PasswordsMismatch))
        gameUpdated = game.copy(
          players = game.players :+ user,
          game = game.game.withPlayer(emptyPlayer(user.name))
        )
        updatedGames = patchGame(gameUpdated, currentGames)
        _ <- setAndStoreGames(updatedGames)
      } yield updatedGames
    ).either

  override def removePlayer(
      requester: User,
      gameId: String
  ): ZIO[Any, Nothing, Either[ErrorADT, MenuGameWithPlayers]] = updateGamesZIO(currentGames =>
    for {
      game <- gameWithId(gameId, currentGames)
      gameUpdated  = game.removePlayer(requester.name)
      updatedGames = patchGame(gameUpdated, currentGames)
      _ <- setAndStoreGames(updatedGames)
    } yield gameUpdated
  ).either

  override def changePlayerInfo(
      user: User,
      gameId: String,
      newPlayerInfo: PlayerInfo
  ): ZIO[Any, Nothing, Either[ErrorADT, MenuGameWithPlayers]] =
    updateGamesZIO(currentGames =>
      for {
        game <- gameWithId(gameId, currentGames)
        _    <- ZIO.unless(game.containsPlayer(user))(ZIO.fail(ErrorADT.YouAreNotInGame(gameId)))
        updatedGame = game.withPlayer(newPlayerInfo)
        newGames    = patchGame(updatedGame, currentGames)
        _ <- setAndStoreGames(newGames)
      } yield updatedGame
    ).either

  override def changeGameMetadata(
      requester: User,
      gameId: String,
      gameMetadata: GameConfigMetadata
  ): ZIO[Any, Nothing, Either[ErrorADT, MenuGameWithPlayers]] =
    updateGamesZIO(currentGames =>
      for {
        game <- gameWithId(gameId, currentGames)
        _ <- ZIO.unless(game.game.gameCreator == requester)(
          ZIO.fail(ErrorADT.YouAreNotCreator(requester.name))
        )
        updatedGame = game.copy(game =
          game.game.copy(gameConfiguration = game.game.gameConfiguration.withMetadata(gameMetadata))
        )
        _ <- updateAndStoreGames(patchGame(updatedGame, _))
      } yield updatedGame
    ).either

  private def updateGamesZIO[R, E, A](
      effect: Vector[MenuGameWithPlayers] => ZIO[R, E, A]
  ): ZIO[R, E, A] =
    gameUpdatesSemaphore.withPermit(for {
      gamesAtStart <- menuGames
      result       <- effect(gamesAtStart)
    } yield result)

  private def playerIsAlreadyPlayingCheck(currentGames: Vector[MenuGameWithPlayers], player: User) =
    ZIO.when(currentGames.exists(_.containsPlayer(player)))(
      ZIO.fail(ErrorADT.UserAlreadyPlaying(player.name))
    )

  private def emptyPlayer(name: String): PlayerInfo =
    Pointed[PlayerInfo].unit.withHumanName(name)

  private def updateAndStoreGames(f: Vector[MenuGameWithPlayers] => Vector[MenuGameWithPlayers]) =
    (for {
      newGames <- gamesRef.updateAndGet(f)
      _        <- storage.storeAtFor(MenuGames.storageKey, newGames, Duration.Inf)
    } yield newGames).orDie

  private def setAndStoreGames(games: Vector[MenuGameWithPlayers]) = updateAndStoreGames(_ => games)

  private def patchGame(game: MenuGameWithPlayers, currentGames: Vector[MenuGameWithPlayers]) =
    currentGames.map(existing => if existing.id == game.id then game else existing)

  private def gameWithId(gameId: String, currentGames: Vector[MenuGameWithPlayers]) = currentGames
    .find(_.id == gameId)
    .map(ZIO.succeed(_))
    .getOrElse(ZIO.fail(ErrorADT.GameDoesNotExist(gameId)))

  private def gameCredentialsStoringKey(gameId: String) =
    LocalStorage.key[GameCredentialsWithGameInfo](s"game-credentials-$gameId")

}

object BMenuGames {}
