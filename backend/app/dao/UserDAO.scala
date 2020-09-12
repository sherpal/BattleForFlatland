package dao

import java.util.concurrent.TimeUnit

import errors.ErrorADT
import errors.ErrorADT.{PendingRegistrationDoesNotExist, UserExists}
import guards.Guards
import io.circe.generic.auto._
import io.circe.syntax._
import models.users.{LoginUser, NewUser, User}
import play.api.mvc.{AnyContent, Request, Result, Results}
import scalatags.Text.all._
import services.config.Configuration
import services.crypto.Crypto
import services.database.users._
import services.emails._
import services.logging._
import utils.WriteableImplicits._
import utils.playzio.HasRequest
import utils.playzio.PlayZIO.simpleZIORequest
import utils.ziohelpers._
import zio.clock.{currentTime, Clock}
import zio.{Has, UIO, ZIO}

object UserDAO extends Results {

  def allUsers(
      from: Long,
      to: Long
  ): ZIO[Users with Clock with Configuration with Has[HasRequest[Request, AnyContent]], ErrorADT, Vector[User]] =
    (for {
      req <- Guards.authenticated[AnyContent]
      _   <- Guards.onlySuperUser(req)
      urs <- users(from, to)
    } yield urs).refineOrDie(ErrorADT.onlyErrorADT)

  val register
      : ZIO[Emails with Logging with Users with Crypto with Clock with Has[HasRequest[Request, NewUser]], ErrorADT, UserDAO.Status] =
    (for {
      request <- simpleZIORequest[NewUser]
      NewUser(userName, password, _, email) = request.body
      maybePendingRegistrationWithEmailFiber <- selectPendingRegistrationByEmail(email).fork
      userAlreadyExistsFiber                 <- userExists(userName).fork // let's pretend validating takes time
      _                                      <- fieldsValidateOrFail(NewUser.fieldsValidator)(request.body)
      userAlreadyExists                      <- userAlreadyExistsFiber.join
      _                                      <- failIfWith(userAlreadyExists, UserExists(userName))
      maybePendingRegistration               <- maybePendingRegistrationWithEmailFiber.join
      _ <- maybePendingRegistration match {
        case Some(pendingRegistration) =>
          removePendingRegistration(pendingRegistration.registrationKey)
        case _ =>
          UIO.succeed(0)
      }
      registrationKey <- addPendingRegistration(userName, password, email)
      _               <- log.info(s"New registration with key `$registrationKey` for user $userName.")
      _               <- sendEmail(email, "Thank you for signing up to Battle for Flatland", div(registrationKey))
    } yield Ok).refineOrDie(ErrorADT.onlyErrorADT)

  def confirmRegistration(
      registrationKey: String
  ): ZIO[Logging with Users with Clock with Crypto, ErrorADT, UserDAO.Status] =
    (for {
      (userAdded, pendingRemoved) <- confirmPendingRegistration(registrationKey)
      _                           <- log.info(s"User added ($userAdded), pending registration removed ($pendingRemoved).")
    } yield Ok).refineOrDie(ErrorADT.onlyErrorADT)

  /**
    * Returns the registration key corresponding to the given `userName`.
    *
    * /!\ This effect will only be used in development, waiting for the mailing service to be set up.
    */
  def registrationKeyFromName(
      userName: String
  ): ZIO[Users, ErrorADT, Result] =
    for {
      maybePendingRegistration <- selectPendingRegistrationByUserName(userName).refineOrDie(ErrorADT.onlyErrorADT)
      pendingRegistration      <- getOrFail(maybePendingRegistration, PendingRegistrationDoesNotExist(userName))
    } yield Ok(pendingRegistration.registrationKey)

  val login: ZIO[Clock with Users with Crypto with Logging with Has[HasRequest[Request, LoginUser]], ErrorADT, Result] =
    (for {
      request <- simpleZIORequest[LoginUser]
      LoginUser(userName, password) = request.body
      user <- correctPassword(userName, password)
      userJson = user.forgetPassword.asJson.noSpaces
      now <- currentTime(TimeUnit.SECONDS)
      _   <- log.info(s"New login by ${request.body.userName}")
    } yield Guards.applySession(Ok, userJson, now.toString)).refineOrDie(ErrorADT.onlyErrorADT)

  val amISuperUser: ZIO[Clock with Configuration with Has[HasRequest[Request, AnyContent]], ErrorADT, UserDAO.Status] =
    (for {
      request        <- simpleZIORequest[AnyContent]
      sessionRequest <- Guards.authenticated(request)
      _              <- Guards.onlySuperUser(sessionRequest)
    } yield Ok).refineOrDie(ErrorADT.onlyErrorADT)

  val me: ZIO[Clock with Configuration with Has[HasRequest[Request, AnyContent]], ErrorADT, Result] = (for {
    session <- Guards.authenticated[AnyContent]
    user    <- UIO(session.user.forgetPassword)
    now     <- currentTime(TimeUnit.SECONDS)
  } yield Guards.applySession(Ok(user), user.asJson.noSpaces, now.toString)).refineOrDie(ErrorADT.onlyErrorADT)

}
