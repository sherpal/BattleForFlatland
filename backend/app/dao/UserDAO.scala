package dao

import java.util.concurrent.TimeUnit

import errors.ErrorADT
import errors.ErrorADT.UserExists
import guards.Guards
import io.circe.generic.auto._
import io.circe.syntax._
import models.users.{LoginUser, NewUser}
import play.api.mvc.{AnyContent, Request, Result, Results}
import services.config.Configuration
import services.crypto.Crypto
import services.database.users._
import services.logging._
import utils.playzio.HasRequest
import utils.playzio.PlayZIO.simpleZIORequest
import utils.ziohelpers._
import zio.clock.{currentTime, Clock}
import zio.{Has, UIO, ZIO}

object UserDAO extends Results {

  private val onlyErrorADT: PartialFunction[Throwable, ErrorADT] = { case e: ErrorADT => e }

  val register
      : ZIO[Logging with Users with Crypto with Clock with Has[HasRequest[Request, NewUser]], ErrorADT, UserDAO.Status] =
    (for {
      request <- simpleZIORequest[NewUser]
      NewUser(userName, password, _, email) = request.body
      maybePendingRegistrationWithEmailFiber <- selectPendingRegistrationByEmail(email).fork
      userAlreadyExistsFiber <- userExists(userName).fork // let's pretend validating takes time
      _ <- fieldsValidateOrFail(NewUser.fieldsValidator)(request.body)
      userAlreadyExists <- userAlreadyExistsFiber.join
      _ <- failIfWith(userAlreadyExists, UserExists(userName))
      maybePendingRegistration <- maybePendingRegistrationWithEmailFiber.join
      _ <- maybePendingRegistration match {
        case Some(pendingRegistration) =>
          removePendingRegistration(pendingRegistration.registrationKey)
        case _ =>
          UIO.succeed(0)
      }
      registrationKey <- addPendingRegistration(userName, password, email)
      _ <- log.info(s"New registration with key `$registrationKey`")
      // todo send email with the registration key
    } yield Ok).refineOrDie(onlyErrorADT)

  def confirmRegistration(
      registrationKey: String
  ): ZIO[Logging with Users with Clock with Crypto, ErrorADT, UserDAO.Status] =
    (for {
      (userAdded, pendingRemoved) <- confirmPendingRegistration(registrationKey)
      _ <- log.info(s"User added ($userAdded), pending registration removed ($pendingRemoved).")
    } yield Ok).refineOrDie(onlyErrorADT)

  val login: ZIO[Clock with Users with Crypto with Logging with Has[HasRequest[Request, LoginUser]], ErrorADT, Result] =
    (for {
      request <- simpleZIORequest[LoginUser]
      _ <- log.info(s"New login by ${request.body.userName}")
      LoginUser(userName, password) = request.body
      user <- correctPassword(userName, password)
      userJson = user.asJson.noSpaces
      now <- currentTime(TimeUnit.SECONDS)
    } yield Guards.applySession(Ok, userJson, now.toString)).refineOrDie(onlyErrorADT)

  val amISuperUser: ZIO[Clock with Configuration with Has[HasRequest[Request, AnyContent]], ErrorADT, Result] =
    (for {
      request <- simpleZIORequest[AnyContent]
      sessionRequest <- Guards.authenticated(request)
      _ <- Guards.onlySuperUser(sessionRequest)
    } yield Ok).refineOrDie(onlyErrorADT)

}
