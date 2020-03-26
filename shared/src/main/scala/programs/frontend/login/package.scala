package programs.frontend

import errors.ErrorADT
import models.users.LoginUser
import services.http.{postIgnore, HttpClient}
import zio.{UIO, URIO, ZIO}
import urldsl.language.QueryParameters.dummyErrorImpl._
import io.circe.generic.auto._

package object login {

  @inline final def confirmRegistrationCall(registrationKey: String): URIO[HttpClient, Either[ErrorADT, Int]] =
    (for {
      path <- UIO.succeed(models.users.Routes.confirmRegistration)
      query <- UIO.succeed(param[String]("registrationKey"))
      statusCode <- postIgnore(path, query)(registrationKey)
    } yield statusCode)
      .refineOrDie(ErrorADT.onlyErrorADT)
      .either

  @inline final def login(loginUser: LoginUser): URIO[HttpClient, Either[ErrorADT, Int]] =
    (for {
      path <- UIO.succeed(models.users.Routes.login)
      statusCode <- postIgnore(path, loginUser)
    } yield statusCode)
      .refineOrDie(ErrorADT.onlyErrorADT)
      .either

}
