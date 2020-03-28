package frontend.components.login

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import errors.ErrorADT
import errors.ErrorADT.{ErrorOr, MultipleErrorsMap, WrongStatusCode}
import frontend.components.Component
import frontend.components.forms.SimpleForm
import frontend.router.{Link, RouteDefinitions}
import models.users.NewUser
import models.validators.FieldsValidator
import org.scalajs.dom.html
import org.scalajs.dom.html.{Form, Progress}
import programs.frontend.login._
import services.http.FrontendHttpClient
import zio.{UIO, ZIO}
import services.routing._
import utils.ziohelpers._

/**
  * Component making the form to register (sign-up) to the Battle For Flatland web application.
  */
final class RegisterForm extends Component[html.Form] with SimpleForm[NewUser, ErrorOr[Int]] {
  val initialData: NewUser                          = NewUser.empty
  val validator: FieldsValidator[NewUser, ErrorADT] = NewUser.fieldsValidator

  val nameChanger: Observer[String]     = makeDataChanger((name: String) => _.copy(name         = name))
  val passwordChanger: Observer[String] = makeDataChanger((password: String) => _.copy(password = password))
  val confirmPasswordChanger: Observer[String] = makeDataChanger(
    (password: String) => _.copy(confirmPassword = password)
  )
  val emailChanger: Observer[String] = makeDataChanger((email: String) => _.copy(email = email))

  val $passwordStrengths: Signal[Double] = $formData.map(_.passwordStrength)

  val passwordStrengthBar: ReactiveHtmlElement[Progress] = progress(
    value <-- $passwordStrengths.map(_ * 100).map(_.toInt).map(_.toString),
    max := 100.toString,
    backgroundColor <-- $passwordStrengths.map { // todo: style this properly
      case x if x <= 0.2 => "#ff0000"
      case x if x <= 0.6 => "#ff9900"
      case _             => "#00ff00"
    }
  )

  val program: ZIO[NewUser, Nothing, Either[ErrorADT, Int]] = (for {
    newUser <- ZIO.environment[NewUser]
    statusCode <- register(newUser, validator).provideLayer(FrontendHttpClient.live)
    // this should never fail as it should fail before
    _ <- unsuccessfulStatusCode(statusCode)
    _ <- moveTo(RouteDefinitions.postRegisterRoute)(newUser.name).provideLayer(FRouting.live)
  } yield statusCode).either

  def submitProgram(formData: NewUser): UIO[ErrorOr[Int]] = program.provide(formData)

  implicit val element: ReactiveHtmlElement[Form] = form(
    submit,
    fieldSet(
      label("Username"),
      input(`type` := "text", inContext(elem => onChange.mapTo(elem.ref.value) --> nameChanger))
    ),
    fieldSet(
      label("Password"),
      input(`type` := "password", inContext(elem => onInput.mapTo(elem.ref.value) --> passwordChanger)),
      passwordStrengthBar
    ),
    fieldSet(
      label("Confirm password"),
      input(`type` := "password", inContext(elem => onChange.mapTo(elem.ref.value) --> confirmPasswordChanger))
    ),
    fieldSet(
      label("Email address"),
      input(`type` := "text", inContext(elem => onChange.mapTo(elem.ref.value) --> emailChanger))
    ),
    input(`type` := "submit", value := "Sign-up", disabled <-- $isSubmitting),
    child <-- $submitEvents.map {
      case Left(MultipleErrorsMap(errors)) => errors.toString
      case Left(error)                     => error.toString
      case Right(code)                     => code.toString
    }
  )

  $formData.foreach(println)
  $passwordStrengths.map("Strength: " + _).foreach(println)
}

object RegisterForm {
  def apply(): RegisterForm = new RegisterForm
}
