package services.database.gametables

import errors.ErrorADT.InconsistentMenuGameInDB
import models.bff.outofgame.{DBMenuGame, MenuGame}
import services.database.users.UsersSlickHelper
import slick.jdbc.JdbcProfile
import utils.database.models.DBUser
import utils.database.tables.{MenuGamesTable, UsersTable}
import zio.{IO, UIO, ZIO}

private[gametables] final class MenuGamesSlickHelper(val api: JdbcProfile#API) extends UsersSlickHelper(api) {

  import api._

  private val gameTableQuery = MenuGamesTable.query

  def gamesWithUserFlat(gamesToJoin: Query[MenuGamesTable, DBMenuGame, Seq]): Query[
    (MenuGamesTable, (UsersTable, Rep[Option[(Rep[String], Rep[Option[String]])]])),
    (DBMenuGame, (DBUser, Option[(String, Option[String])])),
    Seq
  ] =
    gamesToJoin
      .join(
        userQuery.joinLeft(usersToRole).on(_.userId === _._1)
      )
      .on(_.creatorId === _._1.userId)

  def unflattenGames(
      gamesFlat: Seq[(DBMenuGame, (DBUser, Option[(String, Option[String])]))]
  ): UIO[List[Either[InconsistentMenuGameInDB, MenuGame]]] =
    IO.foreachParN(2)(
      for {
        dbGameWithUsersFlat <- gamesFlat.groupBy(_._1).toList
        (dbGame, usersFlatWithGame) = dbGameWithUsersFlat // decouple key and values
        usersFlat                   = usersFlatWithGame.map(_._2) // forget key in values
        maybeCreator                = unflattenUsers(usersFlat).headOption // generates the user
        maybeGame                   = maybeCreator.map(dbGame.menuGame) // generates the game
        gameOrError = ZIO
          .fromOption(maybeGame)
          .flatMapError(_ => UIO(InconsistentMenuGameInDB(dbGame.gameId, dbGame.gameName)))
          .either
      } yield gameOrError // adds the error if need be
    )(identity)

}
