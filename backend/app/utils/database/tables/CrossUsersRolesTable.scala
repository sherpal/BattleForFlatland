package utils.database.tables

import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag
import utils.database.models.CrossUserRole

final class CrossUsersRolesTable(tag: Tag) extends Table[CrossUserRole](tag, "cross_users_roles") {

  def roleId = column[String]("role_id")
  def userId = column[String]("user_id")

  def * = (userId, roleId) <> (CrossUserRole.tupled, CrossUserRole.unapply)

}

object CrossUsersRolesTable {

  def query: TableQuery[CrossUsersRolesTable] = TableQuery[CrossUsersRolesTable]

}
