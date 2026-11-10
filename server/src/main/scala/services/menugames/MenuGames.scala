package services.menugames

import errors.ErrorADT
import models.bff.outofgame.MenuGameWithPlayers
import menus.data.User

import java.time.LocalDateTime
import models.bff.outofgame.MenuGame
import models.bff.outofgame.gameconfig.*
import models.syntax.Pointed

import scala.concurrent.duration.Duration
import scala.util.Try
import gamelogic.docs.BossMetadata
import models.bff.outofgame.gameconfig.GameConfiguration.GameConfigMetadata
import menus.data.GameCredentials
import models.bff.ingame.GameUserCredentials
import menus.data.AllGameCredentials
import services.crypto.Crypto
import services.localstorage.LocalStorage
import menus.data.GameCredentialsWithGameInfo
import models.bff.outofgame.PlayerClasses
import utils.misc.RGBColour

/** The games of the menus, kept in memory and in the `storage`.
  *
  * The changes to the games are made one at a time.
  */
final class MenuGames(storage: LocalStorage) {

  println("Initializing MenuGames Service...")

  @volatile private var games: Vector[MenuGameWithPlayers] =
    storage.retrieveFrom(MenuGames.storageKey).getOrElse(Vector.empty)

  def menuGames: Vector[MenuGameWithPlayers] = games

  /** Marks the game as started, and creates the credentials of its game server and its players. The
    * game server is then run by one of the players, see [[programs.launchGame]].
    */
  def launchGame(
      requester: User,
      gameId: String
  ): Either[ErrorADT, GameCredentialsWithGameInfo] = updateGames(currentGames =>
    for {
      game <- gameWithId(gameId, currentGames)
      _    <- Either.cond(!game.started, (), ErrorADT.GameAlreadyLaunched(gameId))
      _ <- Either.cond(game.isGameCreator(requester), (), ErrorADT.YouAreNotCreator(requester.name))
      gameCredentials = GameCredentials(gameId, java.util.UUID.randomUUID().toString)
      userCredentials = game.players.map { user =>
        GameUserCredentials(user.name, gameId, java.util.UUID.randomUUID().toString)
      }
      allGameCredentials  = AllGameCredentials(gameCredentials, userCredentials)
      credentialsWithInfo = GameCredentialsWithGameInfo(allGameCredentials, game.start)
      _ <- Try(
        storage.storeAtFor(gameCredentialsStoringKey(gameId), credentialsWithInfo, Duration.Inf)
      ).toEither.left.map { throwable =>
        throwable.printStackTrace()
        ErrorADT.RawInternalError(throwable.getMessage)
      }
      _ = setAndStoreGames(patchGame(game.start, currentGames))
    } yield credentialsWithInfo
  )

  /** The game could not start after all: it can be launched again. */
  def cancelLaunch(gameId: String): Unit =
    updateAndStoreGames(_.map {
      case game if game.id == gameId => game.copy(started = false)
      case game => game
    })

  def gameCredentials(gameId: String): Option[GameCredentialsWithGameInfo] =
    Try(storage.retrieveFrom(gameCredentialsStoringKey(gameId))).toOption.flatten

  def retrieveAllGameCredentials(
      gameId: String,
      secret: String
  ): Either[ErrorADT, GameCredentialsWithGameInfo] = for {
    gameInfo <- gameCredentials(gameId).toRight(ErrorADT.CouldNotFetchTokenFromGameServer)
    _        <- Either.cond(secret == gameInfo.secret, (), ErrorADT.WrongGameCredentials)
  } yield gameInfo

  def gameEnded(gameId: String, secret: String): Unit =
    if gameCredentials(gameId).exists(_.secret == secret) then deleteGame(gameId)

  def createGame(
      gameName: String,
      maybePassword: Option[String],
      gameCreator: User
  ): Either[ErrorADT, MenuGameWithPlayers] = updateGames(currentGames =>
    for {
      _ <- Either.cond(
        !currentGames.exists(_.game.gameName == gameName),
        (),
        ErrorADT.GameExists(gameName)
      )
      _ <- Either.cond(
        !currentGames.exists(_.containsPlayer(gameCreator)),
        (),
        ErrorADT.UserAlreadyPlaying(gameCreator.name)
      )
      menuGame = MenuGame(
        java.util.UUID.randomUUID().toString,
        gameName,
        maybePassword.map(Crypto.hashPassword(_).pw),
        gameCreator,
        LocalDateTime.now(),
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
      validationErrors = MenuGame.validator.validate(menuGame)
      _ <- Either.cond(validationErrors.isEmpty, (), ErrorADT.MultipleErrorsMap(validationErrors))
      menuGameWithPlayers = MenuGameWithPlayers(menuGame, Vector(gameCreator), false)
      _                   = updateAndStoreGames(_ :+ menuGameWithPlayers)
    } yield menuGameWithPlayers
  )

  def deleteGame(gameId: String): Unit = updateAndStoreGames(_.filterNot(_.id == gameId))

  def joinGame(
      user: User,
      gameId: String,
      maybePassword: Option[String]
  ): Either[ErrorADT, Vector[MenuGameWithPlayers]] = updateGames(currentGames =>
    for {
      _    <- playerIsAlreadyPlayingCheck(currentGames, user)
      game <- gameWithId(gameId, currentGames)
      hashedPassword = maybePassword.map(Crypto.hashPassword)
      _ <- Either.cond(
        !game.game.maybeHashedPassword.fold(false)(pw =>
          !hashedPassword.map(_.pw).contains[String](pw)
        ),
        (),
        ErrorADT.PasswordsMismatch
      )
      gameUpdated = game.copy(
        players = game.players :+ user,
        game = game.game.withPlayer(emptyPlayer(user.name))
      )
      updatedGames = patchGame(gameUpdated, currentGames)
      _            = setAndStoreGames(updatedGames)
    } yield updatedGames
  )

  /** Adds the "next" AI to the game configuration.
    *
    * @param gameId
    *   id of the game
    * @return
    *   maybe an error message if the AI could not be added (mostly because full, or boss has no ai
    *   implemented yet)
    */
  def addAIToGame(gameId: String): Either[ErrorADT, Unit] = updateGames(currentGames =>
    for {
      game <- gameWithId(gameId, currentGames)
      bossMetadata <- BossMetadata
        .maybeMetadataByName(game.game.gameConfiguration.bossName)
        .toRight(
          ErrorADT.BossDoesNotExist(s"Boss ${game.game.gameConfiguration.bossName} does not exist")
        )
      _ <- bossMetadata.maybeAIComposition
        .toRight(ErrorADT.BossDoesNotHaveAIImplemented(bossMetadata.name))

      players        = game.game.gameConfiguration.playersInfo.values.toVector
      currentClasses = players.flatMap(_.maybePlayerClass)
      aiClass <- bossMetadata.nextAvailableAI(currentClasses).toRight(ErrorADT.NoMoreAIAvailable())
      aiIndicesWithThisClass = players
        .map(_.playerName)
        .collect {
          case PlayerName.AIPlayerName(cls, index) if cls == aiClass => index
        }
        .sorted
      name = aiIndicesWithThisClass.zipWithIndex
        .collectFirst {
          case (aiIndex, indexInVector) if aiIndex != indexInVector => indexInVector
        }
        .map(PlayerName.AIPlayerName(aiClass, _))
        .getOrElse(PlayerName.AIPlayerName(aiClass, aiIndicesWithThisClass.length))
      updatedGame = game.withPlayer(
        PlayerInfo(
          name,
          Some(aiClass),
          Some(
            RGBColour.someColours
              .find(colour => !players.flatMap(_.maybePlayerColour).contains[RGBColour](colour))
              .getOrElse(RGBColour.someColours.head)
          ),
          PlayerStatus.Ready,
          PlayerType.ArtificialIntelligence
        )
      )
    } yield setAndStoreGames(patchGame(updatedGame, currentGames))
  )

  def removeAIFromGame(gameId: String, cls: PlayerClasses): Either[ErrorADT, Unit] =
    updateGames(currentGames =>
      for {
        game <- gameWithId(gameId, currentGames)
        maybeFirstAI = game.game.gameConfiguration.playersInfo.values.collectFirst {
          case PlayerInfo(playerName, _, _, _, PlayerType.ArtificialIntelligence) => playerName
        }
        updatedGame = maybeFirstAI.fold(game)(playerName => game.removePlayer(playerName.name))
      } yield setAndStoreGames(patchGame(updatedGame, currentGames))
    )

  def removePlayer(requester: User, gameId: String): Either[ErrorADT, MenuGameWithPlayers] =
    updateGames(currentGames =>
      for {
        game <- gameWithId(gameId, currentGames)
        gameUpdated = game.removePlayer(requester.name)
        _           = setAndStoreGames(patchGame(gameUpdated, currentGames))
      } yield gameUpdated
    )

  def changePlayerInfo(
      user: User,
      gameId: String,
      newPlayerInfo: PlayerInfo
  ): Either[ErrorADT, MenuGameWithPlayers] = updateGames(currentGames =>
    for {
      game <- gameWithId(gameId, currentGames)
      _    <- Either.cond(game.containsPlayer(user), (), ErrorADT.YouAreNotInGame(gameId))
      updatedGame = game.withPlayer(newPlayerInfo)
      _           = setAndStoreGames(patchGame(updatedGame, currentGames))
    } yield updatedGame
  )

  def changeGameMetadata(
      requester: User,
      gameId: String,
      gameMetadata: GameConfigMetadata
  ): Either[ErrorADT, MenuGameWithPlayers] = updateGames(currentGames =>
    for {
      game <- gameWithId(gameId, currentGames)
      _ <- Either.cond(
        game.game.gameCreator == requester,
        (),
        ErrorADT.YouAreNotCreator(requester.name)
      )
      bossChanged = game.game.gameConfiguration.bossName != gameMetadata.bossName
      updatedGame = game.copy(game =
        game.game.copy(gameConfiguration = game.game.gameConfiguration.withMetadata(gameMetadata))
      )
      _ = updateAndStoreGames(
        patchGame(if bossChanged then updatedGame.removeAllAIs else updatedGame, _)
      )
    } yield updatedGame
  )

  /** Runs `update` on the current games, while no other update can happen. */
  private def updateGames[A](update: Vector[MenuGameWithPlayers] => A): A =
    synchronized(update(games))

  private def playerIsAlreadyPlayingCheck(currentGames: Vector[MenuGameWithPlayers], player: User) =
    Either.cond(
      !currentGames.exists(_.containsPlayer(player)),
      (),
      ErrorADT.UserAlreadyPlaying(player.name)
    )

  private def emptyPlayer(name: String): PlayerInfo =
    Pointed[PlayerInfo].unit.withHumanName(name)

  private def updateAndStoreGames(
      f: Vector[MenuGameWithPlayers] => Vector[MenuGameWithPlayers]
  ): Unit = synchronized {
    games = f(games)
    storage.storeAtFor(MenuGames.storageKey, games, Duration.Inf)
  }

  private def setAndStoreGames(games: Vector[MenuGameWithPlayers]): Unit =
    updateAndStoreGames(_ => games)

  private def patchGame(game: MenuGameWithPlayers, currentGames: Vector[MenuGameWithPlayers]) =
    currentGames.map(existing => if existing.id == game.id then game else existing)

  private def gameWithId(
      gameId: String,
      currentGames: Vector[MenuGameWithPlayers]
  ): Either[ErrorADT, MenuGameWithPlayers] =
    currentGames.find(_.id == gameId).toRight(ErrorADT.GameDoesNotExist(gameId))

  private def gameCredentialsStoringKey(gameId: String) =
    LocalStorage.key[GameCredentialsWithGameInfo](s"game-credentials-$gameId")

}

object MenuGames {

  private val storageKey = LocalStorage.key[Vector[MenuGameWithPlayers]]("menu-games")
}
