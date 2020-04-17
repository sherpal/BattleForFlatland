package utils.database.tables

import models.bff.ingame.GameCredentials
import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

final class GameCredentialsTable(tag: Tag) extends Table[GameCredentials](tag, "game_credentials") {

  def gameId     = column[String]("game_id")
  def gameSecret = column[String]("game_secret")

  def * = (gameId, gameSecret) <> (GameCredentials.tupled, GameCredentials.unapply)

}

object GameCredentialsTable {

  def query: TableQuery[GameCredentialsTable] = TableQuery[GameCredentialsTable]

}
