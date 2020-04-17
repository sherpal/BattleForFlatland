package utils.database.tables

import models.bff.ingame.{GameCredentials, GameUserCredentials}
import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

final class GameUserCredentialsTable(tag: Tag) extends Table[GameUserCredentials](tag, "game_user_credentials") {

  def userId     = column[String]("user_id")
  def gameId     = column[String]("game_id")
  def userSecret = column[String]("user_secret")

  def * = (userId, gameId, userSecret) <> (GameUserCredentials.tupled, GameUserCredentials.unapply)

}

object GameUserCredentialsTable {

  def query: TableQuery[GameUserCredentialsTable] = TableQuery[GameUserCredentialsTable]

}
