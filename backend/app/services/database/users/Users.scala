package services.database.users

import errors.ErrorADT.{CantDeleteTheBoss, IncorrectPassword, UserExists}
import models.{Role, User}
import services.config._
import services.crypto.{HashedPassword, _}
import services.database.db.Database
import services.database.db.Database.DBProvider
import utils.database.DBProfile
import utils.database.models.DBUser
import zio.clock.{currentDateTime, Clock}
import zio.{Task, ZIO, ZLayer}

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
      * Gets the user from their name, if it exists.
      */
    def selectDBUser(userName: String): zio.Task[Option[DBUser]]

    /**
      * Deletes the user with the given name.
      */
    def deleteUser(userName: String): zio.Task[Int]

    /**
      * Returns whether the user with the given userName exists.
      */
    def userExists(userName: String): zio.Task[Boolean] = selectDBUser(userName).map(_.isDefined)

    /**
      * If there is no user with the given userName, creates a new one to the database.
      */
    final def addUserIfNotExists(
        userName: String,
        password: String,
        mailAddress: String
    ): ZIO[Crypto with Clock, Throwable, Boolean] =
      for {
        exists <- userExists(userName)
        _ <- if (exists) ZIO.fail(UserExists(userName)) else ZIO.succeed(())
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
        _ <- if (userName == admin) ZIO.fail(CantDeleteTheBoss) else ZIO.succeed(())
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
