package utils.database.tables

import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag
import utils.database.models.DBRole

final class RolesTable(tag: Tag) extends Table[DBRole](tag, "roles") {

  def roleId   = column[String]("role_id", O.PrimaryKey)
  def roleName = column[String]("role_name")

  def * = (roleId, roleName) <> (DBRole.tupled, DBRole.unapply)

}

object RolesTable {

  def query: TableQuery[RolesTable] = TableQuery[RolesTable]

}
