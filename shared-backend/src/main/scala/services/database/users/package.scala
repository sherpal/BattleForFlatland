package services.database

import models.users.{Role, User}
import services.crypto.Crypto
import utils.database.models.{DBUser, PendingRegistration}
import zio.clock.Clock
import zio.{Has, ZIO}

/**
  * Contains all functions from the Users module.
  * Commented functions should probably not be called from outside (we'll see).
  */
package object users {

  type Users = Has[Users.Service]

  def dbUsers: ZIO[Users, Throwable, Vector[DBUser]] = ZIO.accessM(_.get[Users.Service].dbUsers)

  def users(from: Long, to: Long): ZIO[Users, Throwable, Vector[User]] =
    ZIO.accessM(_.get[Users.Service].users(from, to))

  def insertRoles(roles: List[Role]): ZIO[Users, Throwable, Boolean] =
    ZIO.accessM(_.get[Users.Service].insertRoles(roles))

  def allRoles: ZIO[Users, Throwable, Vector[Role]] = ZIO.accessM(_.get[Users.Service].allRoles)

  //def addRawDBUser(dbUser: DBUser):ZIO[Users, Throwable, Int] = ZIO.accessM(_.get.addRawDBUser(dbUser))

  final def addUser(
      userName: String,
      rawPassword: String,
      mailAddress: String
  ): ZIO[Users with Crypto with Clock, Throwable, Int] =
    ZIO.accessM(_.get[Users.Service].addUser(userName: String, rawPassword: String, mailAddress: String))

  final def userExists(userName: String): ZIO[Users, Throwable, Boolean] =
    ZIO.accessM(_.get[Users.Service].userExists(userName))

  def selectUser(userName: String): ZIO[Users, Throwable, Option[User]] =
    ZIO.accessM(_.get[Users.Service].selectUser(userName))

  def giveUserRoles(userName: String, roles: List[Role]): ZIO[Users, Throwable, Boolean] =
    ZIO.accessM(_.get[Users.Service].giveUserRoles(userName, roles))

  def giveUserRoles(userName: String, role: Role, rolesN: Role*): ZIO[Users, Throwable, Boolean] =
    giveUserRoles(userName, role +: rolesN.toList)

  final def correctPassword(userName: String, password: String): ZIO[Users with Crypto, Throwable, User] =
    ZIO.accessM(_.get[Users.Service].correctPassword(userName, password))

  final def confirmPendingRegistration(
      registrationKey: String
  ): ZIO[Users with Clock with Crypto, Throwable, (Int, Int)] =
    ZIO.accessM(_.get[Users.Service].confirmPendingRegistration(registrationKey))

  def addPendingRegistration(
      userName: String,
      rawPassword: String,
      mailAddress: String
  ): ZIO[Users with Crypto with Clock, Throwable, String] =
    ZIO.accessM(_.get[Users.Service].addPendingRegistration(userName, rawPassword, mailAddress))

  def selectPendingRegistrationByUserName(userName: String): ZIO[Users, Throwable, Option[PendingRegistration]] =
    ZIO.accessM(_.get[Users.Service].selectPendingRegistrationByUserName(userName))

  def selectPendingRegistrationByEmail(email: String): ZIO[Users, Throwable, Option[PendingRegistration]] =
    ZIO.accessM(_.get[Users.Service].selectPendingRegistrationByEmail(email))

  def removePendingRegistration(registrationKey: String): ZIO[Users, Throwable, Int] =
    ZIO.accessM(_.get[Users.Service].removePendingRegistration(registrationKey))

}
