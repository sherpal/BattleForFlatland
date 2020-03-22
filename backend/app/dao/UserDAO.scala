package dao

import java.util.concurrent.TimeUnit

import errors.ErrorADT
import guards.{Guards, SessionRequest}
import models.users.LoginUser
import play.api.mvc.{AnyContent, Request, Result, Results}
import utils.playzio.PlayZIO.zioRequest
import services.database.users._
import io.circe.generic.auto._
import io.circe.syntax._
import models.Role.SuperUser
import services.config.Configuration
import services.crypto.Crypto
import utils.playzio.HasRequest
import zio.{Has, ZIO}
import zio.clock.{currentTime, Clock}

object UserDAO extends Results {

  private val onlyErrorADT: PartialFunction[Throwable, ErrorADT] = { case e: ErrorADT => e }

  val login: ZIO[Clock with Users with Crypto with Has[HasRequest[Request, LoginUser]], ErrorADT, Result] = (for {
    request <- zioRequest[Request, LoginUser]
    userName = request.body.userName
    password = request.body.password
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
