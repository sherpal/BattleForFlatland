package services.database.users

import models.users.{Role, User}
import slick.jdbc.JdbcProfile
import utils.database.models.DBUser
import utils.database.tables.{CrossUsersRolesTable, PendingRegistrationsTable, RolesTable, UsersTable}

abstract class UsersSlickHelper(api: JdbcProfile#API) {

  import api._

  //import DBProfile.api._

  protected val userQuery     = UsersTable.query
  protected val roleQuery     = RolesTable.query
  protected val crossQuery    = CrossUsersRolesTable.query
  protected val pendingRQuery = PendingRegistrationsTable.query

  protected val usersToRole = crossQuery
    .joinLeft(roleQuery)
    .on(_.roleId === _.roleId)
    .map { case (cross, maybeRole) => (cross.userId, maybeRole.map(_.roleName)) }

  /**
    * Given a flat result from the database, with a sequence of [[utils.database.models.DBUser]] and maybe couples
    * of RoleId, and maybeRoleName, computes the corresponding concrete [[models.users.User]].
    */
  protected def unflattenUsers(allUsersWithRolesFlat: Seq[(DBUser, Option[(String, Option[String])])]): Vector[User] =
    allUsersWithRolesFlat
      .groupBy(_._1)
      .map {
        case (user, userRoles) =>
          user.user(userRoles.flatMap(_._2.flatMap(_._2)).map(Role.roleByName).toList)
      }
      .toVector

}
