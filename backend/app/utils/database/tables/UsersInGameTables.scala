package utils.database.tables

import java.time.LocalDateTime

import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag
import utils.database.models.UserInGameTable

final class UsersInGameTables(tag: Tag) extends Table[UserInGameTable](tag, "users_in_game_tables") {

  def gameId   = column[String]("game_id")
  def userId   = column[String]("user_id")
  def joinedOn = column[LocalDateTime]("joined_on")

  def * = (gameId, userId, joinedOn) <> (UserInGameTable.tupled, UserInGameTable.unapply)

}

object UsersInGameTables {

  def query: TableQuery[UsersInGameTables] = TableQuery[UsersInGameTables]

}
