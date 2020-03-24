package services.database.users

import java.util.UUID

import errors.ErrorADT.UserDoesNotExist
import models.{Role, User}
import services.database.db.Database.runAsTask
import slick.jdbc.JdbcProfile
import utils.database.models.{CrossUserRole, DBRole, DBUser, PendingRegistration}
import utils.database.tables.{CrossUsersRolesTable, PendingRegistrationsTable, RolesTable, UsersTable}
import zio.{Task, ZIO}

private[users] final class UsersLive(
    api: JdbcProfile#API
)(implicit db: JdbcProfile#Backend#Database)
    extends Users.Service {
  import api._

  private val userQuery     = UsersTable.query
  private val roleQuery     = RolesTable.query
  private val crossQuery    = CrossUsersRolesTable.query
  private val pendingRQuery = PendingRegistrationsTable.query

  private val usersToRole = crossQuery
    .joinLeft(roleQuery)
    .on(_.roleId === _.roleId)
    .map { case (cross, maybeRole) => (cross.userId, maybeRole.map(_.roleName)) }

  def dbUsers: Task[Vector[DBUser]] =
    runAsTask(
      userQuery.result
    ).map(_.toVector)

  def users(from: Long, to: Long): Task[Vector[User]] =
    runAsTask(
      userQuery.drop(from).take(to - from).joinLeft(usersToRole).on(_.userId === _._1).result
    ).map { allUsersWithRolesFlat =>
      allUsersWithRolesFlat
        .groupBy(_._1)
        .map {
          case (user, userRoles) =>
            user.user(userRoles.flatMap(_._2.flatMap(_._2)).map(Role.roleByName).toList)
        }
        .toVector
    }

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
    ).map { userRoles =>
      userRoles.headOption.map(_._1).map { _.user(userRoles.flatMap(_._2.flatMap(_._2)).map(Role.roleByName).toList) }
    }

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
}
