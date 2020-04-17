package services.database.gamecredentials

import models.bff.ingame.{GameCredentials, GameUserCredentials}
import services.database.db.Database.runAsTask
import slick.jdbc.JdbcProfile
import utils.database.tables.{GameCredentialsTable, GameUserCredentialsTable}
import zio.Task

final class GameCredentialsDBLive(
    val api: JdbcProfile#API
)(implicit db: JdbcProfile#Backend#Database)
    extends GameCredentialsDB.Service {

  import api._

  private def gameCredsQuery     = GameCredentialsTable.query
  private def gameUserCredsQuery = GameUserCredentialsTable.query

  protected def addGameCredentials(gameCredentials: GameCredentials): Task[Int] = runAsTask(
    gameCredsQuery += gameCredentials
  )

  protected def removeGameCredentials(gameId: String): Task[Int] = runAsTask(
    gameCredsQuery.filter(_.gameId === gameId).delete
  )

  protected def addGameUserCredentials(credentials: List[GameUserCredentials]): Task[Option[Int]] = runAsTask(
    gameUserCredsQuery ++= credentials
  )

  protected def removeGameUserCredentials(gameId: String): Task[Int] = runAsTask(
    gameUserCredsQuery.filter(_.gameId === gameId).delete
  )
}
