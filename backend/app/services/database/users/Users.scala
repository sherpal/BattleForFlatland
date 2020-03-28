package services.database.users

import errors.ErrorADT.{
  CantDeleteTheBoss,
  IncorrectPassword,
  PendingRegistrationDoesNotExist,
  PendingRegistrationNotAdded,
  UserExists
}
import models.users.{Role, User}
import services.config._
import services.crypto.{HashedPassword, _}
import services.database.db.Database
import services.database.db.Database.DBProvider
import utils.database.DBProfile
import utils.database.models.{DBUser, PendingRegistration}
import utils.ziohelpers._
import zio.clock.{currentDateTime, Clock}
import zio.{Task, UIO, ZIO, ZLayer}

object Users {

  trait Service {

    def dbUsers: Task[Vector[DBUser]]

    def users(from: Long, to: Long): Task[Vector[User]]

    def insertRoles(roles: List[Role]): Task[Boolean]

    def allRoles: Task[Vector[Role]]

    /**
      * Adds the raw db user to the database.
      * This is assuming that the password already have been hashed, the timestamp set correctly and the
      * unique id already generated.
      */
    def addRawDBUser(dbUser: DBUser): Task[Int]

    /**
      * Creates a new user with the given userName and rawPassword.
      *
      * The rawPassword is the actual password that the user entered when registering.
      */
    final def addUser(
        userName: String,
        rawPassword: String,
        mailAddress: String
    ): ZIO[Crypto with Clock, Throwable, Int] =
      for {
        time <- currentDateTime
        hashed <- hashPassword(rawPassword)
        id <- uuid
        user = DBUser(id, userName, hashed.pw, mailAddress, time.toLocalDateTime)
        added <- addRawDBUser(user)
      } yield added

    /**
      * Adds the raw pending registration to the database
      * This is assuming that the password already have been hashed, the timestamp correctly set and the
      * registration key already generated.
      */
    def addRawPendingRegistration(pendingRegistration: PendingRegistration): Task[Int]

    /**
      * Removes from the database the pending registration with the given key.
      */
    def removePendingRegistration(registrationKey: String): Task[Int]

    /**
      * Given the information sent by the user, adds a pending registration, to be confirmed later.
      * Returns the registration key that have been used for the pending registration, to be notified to
      * the user.
      */
    final def addPendingRegistration(
        userName: String,
        rawPassword: String,
        mailAddress: String
    ): ZIO[Crypto with Clock, Throwable, String] =
      for {
        time <- currentDateTime
        id <- uuid
        registrationKey = id.filterNot(_ == '-')
        hashed <- hashPassword(rawPassword)
        pendingRegistration = PendingRegistration(
          registrationKey,
          userName,
          hashed.pw,
          mailAddress,
          time.toLocalDateTime
        )
        added <- addRawPendingRegistration(pendingRegistration)
        _ <- failIfWith(added == 0, PendingRegistrationNotAdded(userName))
      } yield registrationKey

    final def confirmPendingRegistration(registrationKey: String): ZIO[Clock with Crypto, Throwable, (Int, Int)] =
      for {
        maybePendingRegistration <- selectPendingRegistrationByKey(registrationKey)
        pendingRegistration <- maybePendingRegistration match {
          case Some(e) => UIO.succeed(e)
          case None    => ZIO.fail(PendingRegistrationDoesNotExist(registrationKey))
        }
        PendingRegistration(_, userName, hashed, email, _) = pendingRegistration
        id <- uuid
        now <- currentDateTime
        dbUser = DBUser(id, userName, hashed, email, now.toLocalDateTime)
        added <- addRawDBUser(dbUser)
        removed <- removePendingRegistration(registrationKey)
      } yield (added, removed)

    /**
      * Gets the user from their name, if it exists.
      */
    def selectDBUser(userName: String): zio.Task[Option[DBUser]]

    /**
      * Gets the user from their email, if it exists.
      */
    def selectDBUserByEmail(email: String): Task[Option[DBUser]]

    def selectPendingRegistrationByUserName(userName: String): Task[Option[PendingRegistration]]
    def selectPendingRegistrationByKey(registrationKey: String): Task[Option[PendingRegistration]]
    def selectPendingRegistrationByEmail(email: String): Task[Option[PendingRegistration]]

    /**
      * Deletes the user with the given name.
      */
    def deleteUser(userName: String): zio.Task[Int]

    /**
      * Returns whether the user with the given userName exists.
      */
    def userExists(userName: String): zio.Task[Boolean] =
      for {
        user <- selectDBUser(userName)
        pending <- selectPendingRegistrationByUserName(userName)
      } yield user.isDefined || pending.isDefined

    /**
      * Returns whether this mail already exists in DB.
      */
    def mailExists(email: String): Task[Boolean] =
      for {
        user <- selectDBUserByEmail(email)
        pending <- selectPendingRegistrationByEmail(email)
      } yield user.isDefined || pending.isDefined

    /**
      * If there is no user with the given userName, creates a new one to the database.
      */
    final def addUserIfNotExists(
        userName: String,
        password: String,
        mailAddress: String
    ): ZIO[Crypto with Clock, Throwable, Boolean] =
      for {
        mailExistsFiber <- mailExists(mailAddress).fork
        exists <- userExists(userName)
        _ <- failIfWith(exists, UserExists(userName))
        doesMailExists <- mailExistsFiber.join
        _ <- failIfWith(doesMailExists, UserExists(userName))
        added <- addUser(userName, password, mailAddress).map(_ > 0)
      } yield added

    /** Returns the user if the password was correct, None otherwise. */
    final def correctPassword(userName: String, password: String): ZIO[Crypto, Throwable, User] =
      for {
        maybeUser <- selectUser(userName)
        maybeHashedInDB = maybeUser.map(_.hashedPassword).map(HashedPassword)
        _ <- maybeHashedInDB match {
          case Some(hashedInDB) => checkPassword(password, hashedInDB).filterOrFail(identity)(IncorrectPassword)
          case _                => ZIO.fail(IncorrectPassword)

        }
      } yield maybeUser.get

    /** Deletes the given user if it is not the super user. */
    final def deleteNonSuperUser(userName: String): ZIO[Configuration, Throwable, Int] =
      for {
        admin <- superUserName
        _ <- failIfWith(userName == admin, CantDeleteTheBoss)
        deleted <- deleteUser(userName)
      } yield deleted

    def selectUser(userName: String): Task[Option[User]]

    def userIdFromName(userName: String): Task[Option[String]]

    final def giveUserRoles(userName: String, role1: Role, roleN: Role*): Task[Boolean] =
      giveUserRoles(userName, role1 +: roleN.toList)

    def giveUserRoles(userName: String, roles: List[Role]): Task[Boolean]

  }

  def live: ZLayer[DBProvider, Nothing, Users] =
    ZLayer.fromFunctionM(
      (dbProvider: Database.DBProvider) => dbProvider.get.db.map(db => new UsersLive(DBProfile.api)(db))
    )

}
