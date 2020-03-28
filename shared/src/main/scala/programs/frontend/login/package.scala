package programs.frontend

import errors.ErrorADT
import errors.ErrorADT.WrongStatusCode
import io.circe.generic.auto._
import models.users.{LoginUser, NewUser, RouteDefinitions, User}
import models.validators.FieldsValidator
import services.http._
import urldsl.language.QueryParameters.dummyErrorImpl._
import utils.ziohelpers.fieldsValidateOrFail
import zio.{UIO, URIO, ZIO}
import services.routing._

package object login {

  final def confirmRegistrationCall(registrationKey: String): URIO[HttpClient, Either[ErrorADT, Int]] =
    (for {
      path <- UIO.succeed(models.users.Routes.confirmRegistration)
      query <- UIO.succeed(param[String]("registrationKey"))
      statusCode <- postIgnore(path, query)(registrationKey)
    } yield statusCode)
      .refineOrDie(ErrorADT.onlyErrorADT)
      .either

  final def login(loginUser: LoginUser): ZIO[HttpClient, ErrorADT, Int] =
    (for {
      path <- UIO.succeed(models.users.Routes.login)
      statusCode <- postIgnore(path, loginUser)
    } yield statusCode)
      .refineOrDie(ErrorADT.onlyErrorADT)

  // todo[test]: see that it crashes properly
  final def register(
      newUser: NewUser,
      fieldsValidator: FieldsValidator[NewUser, ErrorADT]
  ): ZIO[HttpClient, ErrorADT, Int] =
    (for {
      _ <- fieldsValidateOrFail(fieldsValidator)(newUser)
      path <- UIO.succeed(models.users.Routes.register)
      statusCode <- postIgnore(path, newUser)
    } yield statusCode)
      .refineOrDie(ErrorADT.onlyErrorADT)

  final val logout: ZIO[Routing with HttpClient, ErrorADT, Unit] =
    for {
      _ <- postIgnore(models.users.Routes.logout, "").refineOrDie(ErrorADT.onlyErrorADT)
      _ <- moveTo(RouteDefinitions.loginRoute)
    } yield ()

  final val me: ZIO[HttpClient, ErrorADT, User] = get[User](models.users.Routes.me).refineOrDie(ErrorADT.onlyErrorADT)

  final val amISuperUser: URIO[HttpClient, Boolean] =
    getStatus(models.users.Routes.superUser).map(_ / 100 == 2).catchAll(_ => UIO(false))

}
