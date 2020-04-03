package frontend.components.login

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import errors.ErrorADT
import errors.ErrorADT.{ErrorOr, MultipleErrorsMap}
import frontend.components.Component
import frontend.components.forms.SimpleForm
import models.users.{NewUser, RouteDefinitions}
import models.validators.FieldsValidator
import org.scalajs.dom.html
import org.scalajs.dom.html.{Form, Progress}
import programs.frontend.login._
import services.http.FrontendHttpClient
import services.routing._
import utils.ziohelpers._
import zio.{UIO, ZIO}
import frontend.components.BootstrapCSS._
import frontend.components.utils.bootstrap.PopperElement

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

  val passwordStrengthBar: ReactiveHtmlElement[html.Div] = div(
    formGroup,
    div(
      className := "progress",
      maxWidth := "200px",
      div(
        className := "progress-bar",
        role := "progressbar",
        aria.valueMin := 0.0,
        aria.valueMax := 100.0,
        aria.valueNow <-- $passwordStrengths.map(_ * 100),
        width <-- $passwordStrengths.map(_ * 100).map(_.toString + "%"),
        className <-- $passwordStrengths.map {
          case x if x <= 0.3 => "bg-danger"
          case x if x <= 0.6 => "bg-warning"
          case _             => "bg-success"
        }
      )
    ),
    span(
      badgePill,
      textInfo,
      "What is this?",
      dataAttr("container") := "body",
      title := "",
      popover,
      placement("right"),
      dataAttr("content") := "This bar attempts to reflects the strength of your password. There is no need to panic if it is not fully green!",
      originalTitle("Password strength"),
      PopperElement.attachedPopover
    )
  )

  val program: ZIO[NewUser, Nothing, Either[ErrorADT, Int]] = (for {
    newUser <- ZIO.environment[NewUser]
    statusCode <- register(newUser, validator).provideLayer(FrontendHttpClient.live)
    // this should never fail as it should fail before
    _ <- unsuccessfulStatusCode(statusCode)
    _ <- moveTo(RouteDefinitions.postRegisterRoute)(newUser.name).provideLayer(FRouting.live)
  } yield statusCode).either

  def submitProgram(formData: NewUser): UIO[ErrorOr[Int]] = program.provide(formData)

  val element: ReactiveHtmlElement[Form] = form(
    submit,
    fieldSet(
      div(
        formGroup,
        label("Username"),
        input(
          `type` := "text",
          placeholder := "Choose user name",
          formControl,
          inContext(elem => onChange.mapTo(elem.ref.value) --> nameChanger)
        )
      ),
      div(
        formGroup,
        label("Password"),
        input(
          `type` := "password",
          placeholder := "Choose password",
          formControl,
          inContext(elem => onInput.mapTo(elem.ref.value) --> passwordChanger)
        ),
        passwordStrengthBar
      ),
      div(
        formGroup,
        label("Confirm password"),
        input(
          `type` := "password",
          placeholder := "Confirm password",
          formControl,
          inContext(elem => onChange.mapTo(elem.ref.value) --> confirmPasswordChanger)
        )
      ),
      div(
        formGroup,
        label("Email address"),
        input(
          `type` := "email",
          placeholder := "Enter your email",
          formControl,
          inContext(elem => onChange.mapTo(elem.ref.value) --> emailChanger)
        )
      ),
      input(`type` := "submit", value := "Sign-up", btnPrimary, disabled <-- $isSubmitting)
    ),
    child <-- $submitEvents.map {
      case Left(MultipleErrorsMap(errors)) => errors.toString
      case Left(error)                     => error.toString
      case Right(code)                     => code.toString
    }
  )
}

object RegisterForm {
  def apply(): RegisterForm = new RegisterForm
}
