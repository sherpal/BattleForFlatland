package programs.frontend.login

import errors.ErrorADT
//import io.circe.generic.auto.*
import models.users.{LoginUser, NewUser, RouteDefinitions, User}
import models.validators.FieldsValidator
import services.http.*
import urldsl.language.QueryParameters.dummyErrorImpl.*
import urldsl.vocabulary.Printer
import utils.ziohelpers.fieldsValidateOrFail
import zio.{UIO, URIO, ZIO}

def confirmRegistrationCall(registrationKey: String): URIO[HttpClient, Either[ErrorADT, Int]] =
  (for {
    path       <- ZIO.succeed(models.users.Routes.confirmRegistration)
    query      <- ZIO.succeed(param[String]("registrationKey"))
    statusCode <- postIgnore(path, query)(registrationKey)
  } yield statusCode)
    .refineOrDie(ErrorADT.onlyErrorADT)
    .either

def login(loginUser: LoginUser): ZIO[HttpClient, ErrorADT, Int] =
  (for {
    path       <- ZIO.succeed(models.users.Routes.login)
    statusCode <- postIgnore(path, loginUser)
  } yield statusCode)
    .refineOrDie(ErrorADT.onlyErrorADT)

// todo[test]: see that it crashes properly
def register(
    newUser: NewUser,
    fieldsValidator: FieldsValidator[NewUser, ErrorADT]
): ZIO[HttpClient, ErrorADT, Int] =
  (for {
    _          <- fieldsValidateOrFail(fieldsValidator)(newUser)
    path       <- ZIO.succeed(models.users.Routes.register)
    statusCode <- postIgnore(path, newUser)
  } yield statusCode)
    .refineOrDie(ErrorADT.onlyErrorADT)

val me: ZIO[HttpClient, ErrorADT, User] = get[User](models.users.Routes.me).refineOrDie(ErrorADT.onlyErrorADT)

val amISuperUser: URIO[HttpClient, Boolean] =
  getStatus(models.users.Routes.superUser).map(_ / 100 == 2).catchAll(_ => ZIO.succeed(false))

given Printer[Long] = (t: Long) => t.toString

def users(from: Long, to: Long): ZIO[HttpClient, ErrorADT, List[User]] =
  get[List[User]](models.users.Routes.donwloadUsers, param[Long]("from") & param[Long]("to"))((from, to))
    .refineOrDie(ErrorADT.onlyErrorADT)
