package dao

import java.util.concurrent.TimeUnit

import errors.ErrorADT
import errors.ErrorADT.UserExists
import guards.{Guards, SessionRequest}
import models.users.{LoginUser, NewUser}
import play.api.mvc.{AnyContent, Request, Result, Results}
import utils.playzio.PlayZIO.zioRequest
import io.circe.generic.auto._
import io.circe.syntax._
import models.Role.SuperUser
import services.config.Configuration
import services.crypto.Crypto
import utils.playzio.HasRequest
import utils.ziohelpers._
import zio.{Has, ZIO}
import zio.clock.{currentTime, Clock}
import services.database.users._

object UserDAO extends Results {

  private val onlyErrorADT: PartialFunction[Throwable, ErrorADT] = { case e: ErrorADT => e }

  val register: ZIO[Users with Crypto with Clock with Has[HasRequest[Request, NewUser]], ErrorADT, Result] = (for {
    request <- zioRequest[Request, NewUser]
    NewUser(userName, password, _, email) = request.body
    userAlreadyExistsFiber <- userExists(userName).fork // let's pretend validating takes time
    _ <- fieldsValidateOrFail(NewUser.fieldsValidator)(request.body)
    userAlreadyExists <- userAlreadyExistsFiber.join
    _ <- failIfWith(userAlreadyExists, UserExists(userName))
    _ <- addUser(userName, password, email)
  } yield Ok).refineOrDie(onlyErrorADT)

  val login: ZIO[Clock with Users with Crypto with Has[HasRequest[Request, LoginUser]], ErrorADT, Result] = (for {
    request <- zioRequest[Request, LoginUser]
    LoginUser(userName, password) = request.body
    user <- correctPassword(userName, password)
    userJson = user.asJson.noSpaces
    now <- currentTime(TimeUnit.SECONDS)
  } yield Guards.applySession(Ok, userJson, now.toString)).refineOrDie(onlyErrorADT)

  val amISuperUser: ZIO[Clock with Configuration with Has[HasRequest[Request, AnyContent]], ErrorADT, Result] =
    (for {
      request <- zioRequest[Request, AnyContent]
      sessionRequest <- Guards.authenticated[AnyContent, Request[AnyContent]](request)
      _ <- Guards.authorized[AnyContent, SessionRequest[AnyContent]](sessionRequest, List(SuperUser))
    } yield Ok).refineOrDie(onlyErrorADT)

}
