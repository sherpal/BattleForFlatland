package services.database.users

import models.users.{Role, User}
import utils.database.models.{DBRole, DBUser, PendingRegistration}
import zio.{Task, UIO, ZLayer}

class UsersMockUp extends Users.Service {

  private var usersListDB: Vector[(DBUser, List[Role])] = Vector()
  private var rolesDB: Vector[DBRole] = Role.roles.zipWithIndex.map {
    case (role, idx) => DBRole(idx.toString, role.name)
  }.toVector
  private var pendingRegistrationsDB: Vector[PendingRegistration] = Vector()

  private implicit class TupleLists[U, V](ls: Vector[(U, V)]) {
    def _1: Vector[U] = ls.map(_._1)
    //def _2: Vector[V] = ls.map(_._2)
  }

  def dbUsers: Task[Vector[DBUser]] = Task.succeed(usersListDB.map(_._1))

  def users(from: Long, to: Long): Task[Vector[User]] = Task.succeed(
    usersListDB
      .map {
        case (user, roles) =>
          user.user(roles)
      }
      .slice(math.max(0, from.toInt), to.toInt)
  )

  def insertRoles(roles: List[Role]): Task[Boolean] =
    for {
      startIdx <- UIO(rolesDB.map(_.roleId.toInt).max + 1)
      dbRoles <- UIO(roles.zipWithIndex.map { case (role, idx) => DBRole((idx + startIdx).toString, role.name) })
      _ <- UIO(rolesDB ++= dbRoles.toVector)
    } yield true

  def allRoles: Task[Vector[Role]] = Task.succeed(rolesDB.map(_.roleName).map(Role.roleByName))

  def addRawDBUser(dbUser: DBUser): Task[Int] =
    if (usersListDB.exists(_._1.userId == dbUser.userId)) Task.fail(new Exception("User Id already exists"))
    else Task.succeed(usersListDB +:= (dbUser, Nil)) *> UIO(1)

  def addRawPendingRegistration(pendingRegistration: PendingRegistration): Task[Int] =
    if (pendingRegistrationsDB.exists(_.registrationKey == pendingRegistration.registrationKey))
      Task.fail(new Exception("Registration key already exists"))
    else Task.succeed(pendingRegistrationsDB +:= pendingRegistration) *> UIO(1)

  def removePendingRegistration(registrationKey: String): Task[Int] =
    for {
      wasThereBefore <- UIO(pendingRegistrationsDB.exists(_.registrationKey == registrationKey))
      _ <- Task.succeed {
        pendingRegistrationsDB = pendingRegistrationsDB.filterNot(_.registrationKey == registrationKey)
      }
    } yield if (wasThereBefore) 1 else 0

  def selectDBUser(userName: String): Task[Option[DBUser]] =
    Task.succeed(usersListDB._1.find(_.userName == userName))

  def selectDBUserByEmail(email: String): Task[Option[DBUser]] =
    Task.succeed(usersListDB._1.find(_.mailAddress == email))

  def selectPendingRegistrationByUserName(userName: String): Task[Option[PendingRegistration]] =
    Task.succeed(pendingRegistrationsDB.find(_.userName == userName))

  def selectPendingRegistrationByKey(registrationKey: String): Task[Option[PendingRegistration]] =
    Task.succeed(pendingRegistrationsDB.find(_.registrationKey == registrationKey))

  def selectPendingRegistrationByEmail(email: String): Task[Option[PendingRegistration]] =
    Task.succeed(pendingRegistrationsDB.find(_.mailAddress == email))

  def deleteUser(userName: String): Task[Int] =
    for {
      wasThereBefore <- UIO(usersListDB._1.find(_.userName == userName).toList.length)
      _ <- Task.succeed {
        usersListDB = usersListDB.filterNot(_._1.userName == userName)
      }
    } yield wasThereBefore

  def selectUser(userName: String): Task[Option[User]] =
    Task.succeed(usersListDB.find(_._1.userName == userName).map { case (user, roles) => user.user(roles) })

  def userIdFromName(userName: String): Task[Option[String]] = selectDBUser(userName).map(_.map(_.userId))

  def giveUserRoles(userName: String, roles: List[Role]): Task[Boolean] =
    for {
      maybeUser <- selectUser(userName)
      _ <- deleteUser(userName)
      _ <- maybeUser match {
        case Some(user) =>
          Task.succeed(
            usersListDB +:= (DBUser(user.userId, user.userName, user.hashedPassword, user.mailAddress, user.createdOn), roles)
          )
        case None => UIO(true)
      }
    } yield maybeUser.isDefined
}

object UsersMockUp {

  def test: ZLayer[Any, Nothing, Users] = ZLayer.succeed(new UsersMockUp)

}
