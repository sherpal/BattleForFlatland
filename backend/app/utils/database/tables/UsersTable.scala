package utils.database.tables

import java.time.LocalDateTime

import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag
import utils.database.models.DBUser

final class UsersTable(tag: Tag) extends Table[DBUser](tag, "users") {

  def userId         = column[String]("user_id", O.PrimaryKey)
  def userName       = column[String]("user_name")
  def hashedPassword = column[String]("hashed_password")
  def mailAddress    = column[String]("mail_address")
  def createdOn      = column[LocalDateTime]("created_on")

  def * = (userId, userName, hashedPassword, mailAddress, createdOn) <> (DBUser.tupled, DBUser.unapply)

}

object UsersTable {

  def query: TableQuery[UsersTable] = TableQuery[UsersTable]

}
