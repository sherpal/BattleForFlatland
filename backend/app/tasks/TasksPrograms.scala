package tasks

import models.users.Role.SuperUser
import models.users.{Role, User}
import services.config._
import services.crypto.Crypto
import services.database.users._
import services.logging._
import zio.ZIO
import zio.clock._

import scala.concurrent.duration._

object TasksPrograms {

  val addingRolesAndSuperUser: ZIO[Logging with Users with Crypto with Clock with Configuration, Throwable, Unit] =
    for {
      _             <- log.info("Adding roles and super user in 10 seconds")
      _             <- sleep(zio.duration.Duration.fromScala(10.seconds))
      _             <- log.info("Going to insert role and super user now.")
      existingRoles <- allRoles
      rolesToAdd = Role.roles.filterNot(existingRoles.contains)
      _              <- insertRoles(rolesToAdd)
      _              <- log.info("Roles have been added.")
      userName       <- superUserName
      password       <- superUserPassword
      mail           <- superUserMail
      maybeSuperUser <- selectUser(userName)
      _ <- maybeSuperUser match {
        case Some(_) => ZIO.succeed(())
        case None    => addUser(userName, password, mail)
      }
      _ <- log.info("Super User has been added.")
      _ <- maybeSuperUser match {
        case Some(User(_, _, _, _, _, roles)) if roles.contains(SuperUser) =>
          ZIO.succeed(())
        case _ =>
          giveUserRoles(userName, SuperUser)
      }
      _ <- log.info("Super User has been granted with their role.")
    } yield ()
}
