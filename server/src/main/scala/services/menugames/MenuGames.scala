package services.menugames

import be.doeraene.utils.castorutils.ask
import errors.ErrorADT
import eventsourcing.{EventSourcingService, Projection}
import models.bff.outofgame.MenuGameWithPlayers
import menus.data.{GameCreatedInfo, GameCredentialsWithGameInfo, User}

import java.time.LocalDateTime
import models.bff.outofgame.MenuGame
import models.bff.outofgame.gameconfig.*
import models.bff.outofgame.PlayerClasses
import models.bff.outofgame.gameconfig.GameConfiguration.GameConfigMetadata
import gamelogic.docs.BossMetadata
import scalasql.simple.{SimpleTable, SqliteDialect}
import services.crypto.Crypto
import services.database.DatabaseService

import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}
import scala.util.Try

/** The games of the menus. Each game is a [[MenuGameEntity]], whose id is given by
  * [[GameCreatedInfo]].
  *
  * The games that are not over are also kept in memory, for the checks that involve all of them
  * (unique names, players in only one game). The changes to the games are made one at a time.
  */
final class MenuGames(eventSourcing: EventSourcingService, database: DatabaseService)(using
    castor.Context
) {
  import MenuGames.*
  import MenuGameEntity.Command

  println("Initializing MenuGames Service...")

  private val gameCreatedInfo = eventSourcing.entity(0, GameCreatedInfo.entityInfo)

  private def entity(gameId: Int) = eventSourcing.entity(gameId, MenuGameEntity.entityInfo)

  private val openGamesProjection =
    eventSourcing.registerProjection(MenuGameEntity.entityInfo, openGames(database))

  @volatile private var games: Vector[MenuGameWithPlayers] = {
    val deadline = timeout.fromNow
    while !await(openGamesProjection.isUpToDate) do {
      if deadline.isOverdue() then
        throw IllegalStateException("The open games could not be loaded in time")
      Thread.sleep(50)
    }
    import SqliteDialect.*
    database.db
      .run(OpenMenuGame.select.sortBy(_.gameId).map(_.gameId))
      .toVector
      .flatMap(gameId =>
        Try(await(entity(gameId).ask(Command.Get(_)))).toOption.flatMap(_.toOption)
      )
      .flatMap(_.maybeGame)
  }

  def menuGames: Vector[MenuGameWithPlayers] = games

  /** Marks the game as started, and creates the credentials of its game server and its players. The
    * game server is then run by one of the players, see [[programs.launchGame]].
    */
  def launchGame(requester: User, gameId: Int): Either[ErrorADT, GameCredentialsWithGameInfo] =
    synchronized {
      for {
        game        <- run(gameId)(Command.Launch(requester, _))
        credentials <- game.maybeCredentials.toRight(ErrorADT.GameDoesNotExist(gameId))
      } yield {
        gameCreatedInfo.ask(GameCreatedInfo.Command.GameWasStarted(_))
        credentials
      }
    }

  /** The game could not start after all: it can be launched again. */
  def cancelLaunch(gameId: Int): Unit = synchronized {
    run(gameId)(Command.CancelLaunch(_))
    ()
  }

  def gameCredentials(gameId: Int): Option[GameCredentialsWithGameInfo] =
    Try(await(entity(gameId).ask(Command.Get(_)))).toOption
      .flatMap(_.toOption)
      .flatMap(_.maybeCredentials)

  def retrieveAllGameCredentials(
      gameId: Int,
      secret: String
  ): Either[ErrorADT, GameCredentialsWithGameInfo] = for {
    gameInfo <- gameCredentials(gameId).toRight(ErrorADT.CouldNotFetchTokenFromGameServer)
    _        <- Either.cond(secret == gameInfo.secret, (), ErrorADT.WrongGameCredentials)
  } yield gameInfo

  def gameEnded(gameId: Int, secret: String): Unit =
    if gameCredentials(gameId).exists(_.secret == secret) then deleteGame(gameId)

  def createGame(
      gameName: String,
      maybePassword: Option[String],
      gameCreator: User
  ): Either[ErrorADT, MenuGameWithPlayers] = synchronized {
    for {
      _ <- Either.cond(
        !games.exists(_.game.gameName == gameName),
        (),
        ErrorADT.GameExists(gameName)
      )
      _ <- playerIsAlreadyPlayingCheck(gameCreator)
      menuGame = MenuGame(
        0, // the id is only taken once the game is valid
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
      gameId <- Try(await(gameCreatedInfo.ask(GameCreatedInfo.Command.NextId(_)))).toEither.left
        .map(internalError)
      game <- runOnGame(gameId)(
        Command.Create(
          MenuGameWithPlayers(menuGame.copy(gameId = gameId), Vector(gameCreator), false),
          _
        )
      )
    } yield game
  }

  def deleteGame(gameId: Int): Unit = synchronized {
    run(gameId)(Command.End(_))
    ()
  }

  def joinGame(
      user: User,
      gameId: Int,
      maybePassword: Option[String]
  ): Either[ErrorADT, Vector[MenuGameWithPlayers]] = synchronized {
    for {
      _ <- playerIsAlreadyPlayingCheck(user)
      _ <- runOnGame(gameId)(Command.Join(user, maybePassword, _))
    } yield games
  }

  /** Adds the "next" AI to the game configuration.
    *
    * @param gameId
    *   id of the game
    * @return
    *   maybe an error message if the AI could not be added (mostly because full, or boss has no ai
    *   implemented yet)
    */
  def addAIToGame(gameId: Int): Either[ErrorADT, Unit] = synchronized {
    runOnGame(gameId)(Command.AddAI(_)).map(_ => ())
  }

  def removeAIFromGame(gameId: Int, cls: PlayerClasses): Either[ErrorADT, Unit] = synchronized {
    runOnGame(gameId)(Command.RemoveAI(cls, _)).map(_ => ())
  }

  def removePlayer(requester: User, gameId: Int): Either[ErrorADT, MenuGameWithPlayers] =
    synchronized {
      runOnGame(gameId)(Command.RemovePlayer(requester.name, _))
    }

  def changePlayerInfo(
      user: User,
      gameId: Int,
      newPlayerInfo: PlayerInfo
  ): Either[ErrorADT, MenuGameWithPlayers] = synchronized {
    runOnGame(gameId)(Command.ChangePlayerInfo(user, newPlayerInfo, _))
  }

  def changeGameMetadata(
      requester: User,
      gameId: Int,
      gameMetadata: GameConfigMetadata
  ): Either[ErrorADT, MenuGameWithPlayers] = synchronized {
    runOnGame(gameId)(Command.ChangeMetadata(requester, gameMetadata, _))
  }

  /** Sends the command to the game, and keeps its new state in memory. Must be called while
    * synchronized.
    */
  private def run(
      gameId: Int
  )(command: MenuGameEntity.Reply => Command): Either[ErrorADT, MenuGameEntity] = {
    val result = Try(await(entity(gameId).ask(command))).toEither.left.map(internalError).flatten
    result.foreach { state =>
      games = state.maybeGame match {
        case Some(game) if games.exists(_.id == gameId) =>
          games.map(existing => if existing.id == gameId then game else existing)
        case Some(game) => games :+ game
        case None => games.filterNot(_.id == gameId)
      }
    }
    result
  }

  private def runOnGame(gameId: Int)(
      command: MenuGameEntity.Reply => Command
  ): Either[ErrorADT, MenuGameWithPlayers] =
    run(gameId)(command).flatMap(_.maybeGame.toRight(ErrorADT.GameDoesNotExist(gameId)))

  private def playerIsAlreadyPlayingCheck(player: User) =
    Either.cond(
      !games.exists(_.containsPlayer(player)),
      (),
      ErrorADT.UserAlreadyPlaying(player.name)
    )

}

object MenuGames {

  private val timeout = 10.seconds

  private def await[A](future: Future[A]): A = Await.result(future, timeout)

  private def internalError(throwable: Throwable): ErrorADT = {
    throwable.printStackTrace()
    ErrorADT.RawInternalError(throwable.getMessage)
  }

  private case class OpenMenuGame(gameId: Int)

  private object OpenMenuGame extends SimpleTable[OpenMenuGame]

  /** Keeps the ids of the games that are not over in the `open_menu_game` table. */
  private def openGames(database: DatabaseService): Projection[MenuGameEntity.Event] =
    Projection[MenuGameEntity.Event]("open-menu-games", Projection.Semantics.AtLeastOnce) {
      (gameId, envelope) =>
        import SqliteDialect.*
        envelope.event match {
          case MenuGameEntity.Event.GameCreated(_) =>
            database.db.run(
              OpenMenuGame.insert.values(OpenMenuGame(gameId)).onConflictIgnore(_.gameId)
            )
          case MenuGameEntity.Event.GameEnded() =>
            database.db.run(OpenMenuGame.delete(_.gameId === gameId))
          case _ => ()
        }
    }
}
