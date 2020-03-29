package services.database.users

import java.util.UUID

import errors.ErrorADT.UserDoesNotExist
import models.users.{Role, User}
import services.database.db.Database.runAsTask
import slick.jdbc.JdbcProfile
import utils.database.models.{CrossUserRole, DBRole, DBUser, PendingRegistration}
import zio.{Task, ZIO}

private[users] final class UsersLive(
    val api: JdbcProfile#API
)(implicit db: JdbcProfile#Backend#Database)
    extends UsersSlickHelper(api)
    with Users.Service {
  import api._

  def dbUsers: Task[Vector[DBUser]] =
    runAsTask(
      userQuery.result
    ).map(_.toVector)

  def users(from: Long, to: Long): Task[Vector[User]] =
    runAsTask(
      userQuery.drop(from).take(to - from).joinLeft(usersToRole).on(_.userId === _._1).result
    ).map(unflattenUsers)

  def insertRoles(roles: List[Role]): Task[Boolean] =
    if (roles.isEmpty) ZIO.succeed(true)
    else
      runAsTask(roleQuery ++= Role.roles.map(_.name).map(UUID.randomUUID().toString -> _).map(DBRole.tupled)).as(true)

  def allRoles: Task[Vector[Role]] =
    runAsTask(roleQuery.result).map(_.toVector.map(dbRole => Role.roleByName(dbRole.roleName)))

  def addRawDBUser(dbUser: DBUser): Task[Int] = runAsTask(userQuery += dbUser)

  def selectDBUser(userName: String): Task[Option[DBUser]] =
    runAsTask(userQuery.filter(_.userName === userName).result.headOption)

  /** Deletes the given User. The database should cascade de delete for all elements pointing to it. */
  def deleteUser(userName: String): Task[Int] =
    runAsTask(userQuery.filter(_.userName === userName).delete)

  def selectUser(userName: String): Task[Option[User]] =
    runAsTask(
      userQuery.filter(_.userName === userName).joinLeft(usersToRole).on(_.userId === _._1).result
    ).map(unflattenUsers).map(_.headOption)

  def userIdFromName(userName: String): Task[Option[String]] =
    runAsTask(userQuery.filter(_.userName === userName).map(_.userId).result.headOption)

  def giveUserRoles(userName: String, roles: List[Role]): Task[Boolean] =
    for {
      maybeUserId <- userIdFromName(userName)
      userId <- maybeUserId match {
        case Some(id) => ZIO.succeed(id)
        case None     => ZIO.fail(UserDoesNotExist(userName))
      }
      newRolesIds <- runAsTask(
        roleQuery
          .filter(_.roleName inSet roles.map(_.name))
          .filterNot(
            _.roleId in crossQuery
              .filter(_.userId === userId)
              .map(_.roleId)
          )
          .map(_.roleId)
          .result
      )
      _ <- runAsTask(
        crossQuery ++= newRolesIds.map(roleId => CrossUserRole(userId, roleId))
      )
    } yield true

  def addRawPendingRegistration(pendingRegistration: PendingRegistration): Task[Int] =
    runAsTask(pendingRQuery += pendingRegistration)

  def removePendingRegistration(registrationKey: String): Task[Int] =
    runAsTask(pendingRQuery.filter(_.registrationKey === registrationKey).delete)

  def selectPendingRegistrationByUserName(userName: String): Task[Option[PendingRegistration]] =
    runAsTask(pendingRQuery.filter(_.userName === userName).result.headOption)

  def selectPendingRegistrationByKey(registrationKey: String): Task[Option[PendingRegistration]] =
    runAsTask(pendingRQuery.filter(_.registrationKey === registrationKey).result.headOption)

  def selectDBUserByEmail(email: String): Task[Option[DBUser]] =
    runAsTask(userQuery.filter(_.mailAddress === email).result.headOption)

  def selectPendingRegistrationByEmail(email: String): Task[Option[PendingRegistration]] =
    runAsTask(pendingRQuery.filter(_.mailAddress === email).result.headOption)
}
