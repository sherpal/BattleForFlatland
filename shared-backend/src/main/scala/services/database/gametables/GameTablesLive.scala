package services.database.gametables

import errors.ErrorADT.InconsistentMenuGameInDB
import io.circe.Encoder
import models.bff.outofgame.gameconfig.GameConfiguration
import models.bff.outofgame.{DBMenuGame, MenuGame}
import models.users.User
import services.database.db.Database.runAsTask
import services.database.users.UsersSlickHelper
import slick.jdbc.JdbcProfile
import utils.database.models.{DBUser, UserInGameTable}
import utils.database.tables.{MenuGamesTable, UsersInGameTables}
import zio.{Task, UIO, ZIO}

final class GameTablesLive(
    val api: JdbcProfile#API
)(implicit db: JdbcProfile#Backend#Database)
    extends UsersSlickHelper(api)
    with GameTable.Service {

  val gamesHelper = new MenuGamesSlickHelper(api)

  import api._

  private val gameTableQuery        = MenuGamesTable.query
  private val usersInGameTableQuery = UsersInGameTables.query

  def gameTables: Task[List[Either[InconsistentMenuGameInDB, MenuGame]]] =
    for {
      gamesFlat <- runAsTask(gamesHelper.gamesWithUserFlat(gameTableQuery).result)
      _ <- UIO(gamesFlat.map((eqsdf: (DBMenuGame, (DBUser, Option[(String, Option[String])]))) => eqsdf))
      games <- gamesHelper.unflattenGames(gamesFlat)
    } yield games

  protected def newDBGame(dbMenuGame: DBMenuGame): Task[Int] = runAsTask(gameTableQuery += dbMenuGame)

  protected def modifyGameConfiguration(gameId: String, configuration: GameConfiguration): Task[Int] =
    runAsTask(
      gameTableQuery
        .map(game => (game.gameId, game.gameConfigurationAsString))
        .update(gameId -> configuration.json)
    )

  def deleteGame(gameName: String): Task[Int] =
    runAsTask(gameTableQuery.filter(_.gameName === gameName).delete)

  def selectGameByName(gameName: String): Task[Option[MenuGame]] =
    for {
      gamesFlat <- runAsTask(gamesHelper.gamesWithUserFlat(gameTableQuery.filter(_.gameName === gameName)).result)
      game <- gamesHelper.unflattenGames(gamesFlat).map(_.headOption).flatMap {
        case Some(Left(error)) => ZIO.fail(error)
        case Some(Right(g))    => UIO(Some(g))
        case None              => UIO(None)
      }
    } yield game

  def selectGameById(gameId: String): Task[Option[MenuGame]] =
    for {
      gamesFlat <- runAsTask(gamesHelper.gamesWithUserFlat(gameTableQuery.filter(_.gameId === gameId)).result)
      game <- gamesHelper.unflattenGames(gamesFlat).map(_.headOption).flatMap {
        case Some(Left(error)) => ZIO.fail(error)
        case Some(Right(g))    => UIO(Some(g))
        case None              => UIO(None)
      }
    } yield game

  protected def addUsersInGameTables(userInGameTable: UserInGameTable): Task[Int] =
    runAsTask(usersInGameTableQuery += userInGameTable)

  protected def removeUsersInGameTables(userInGameTable: UserInGameTable): Task[Int] =
    runAsTask(
      usersInGameTableQuery
        .filter(_.userId === userInGameTable.userId)
        .filter(_.gameId === userInGameTable.gameId)
        .delete
    )

  def userAlreadyPlaying(userId: String): Task[Option[String]] =
    runAsTask(
      usersInGameTableQuery
        .filter(_.userId === userId)
        .map(_.gameId)
        .result
    ).map(_.headOption)

  protected def playersInGameWithId(gameId: String): Task[List[User]] =
    runAsTask(
      userQuery
        .filter(_.userId in usersInGameTableQuery.filter(_.gameId === gameId).map(_.userId))
        .joinLeft(usersToRole)
        .on(_.userId === _._1)
        .result
    ).map(unflattenUsers).map(_.toList)
}
