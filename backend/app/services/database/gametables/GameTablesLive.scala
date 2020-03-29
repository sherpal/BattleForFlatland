package services.database.gametables

import errors.ErrorADT.InconsistentMenuGameInDB
import models.bff.outofgame.{DBMenuGame, MenuGame}
import services.database.db.Database.runAsTask
import services.database.users.UsersSlickHelper
import slick.jdbc.JdbcProfile
import utils.database.models.DBUser
import utils.database.tables.MenuGamesTable
import zio.{IO, Task, UIO, ZIO}
import utils.ziohelpers._

final class GameTablesLive(
    val api: JdbcProfile#API
)(implicit db: JdbcProfile#Backend#Database)
    extends UsersSlickHelper(api)
    with GameTable.Service {

  val gamesHelper = new MenuGamesSlickHelper(api)

  import api._

  private val gameTableQuery = MenuGamesTable.query

  def gameTables: Task[List[Either[InconsistentMenuGameInDB, MenuGame]]] =
    for {
      gamesFlat <- runAsTask(gamesHelper.gamesWithUserFlat(gameTableQuery).result)
      _ <- UIO(gamesFlat.map((eqsdf: (DBMenuGame, (DBUser, Option[(String, Option[String])]))) => eqsdf))
      games <- gamesHelper.unflattenGames(gamesFlat)
    } yield games

  protected def newDBGame(dbMenuGame: DBMenuGame): Task[Int] = runAsTask(gameTableQuery += dbMenuGame)

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
}
