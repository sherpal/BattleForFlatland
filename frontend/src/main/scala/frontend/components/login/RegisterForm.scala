package frontend.components.login

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import errors.ErrorADT
import errors.ErrorADT.{ErrorOr, MultipleErrorsMap}
import frontend.components.Component
import frontend.components.forms.SimpleForm
import frontend.components.utils.popper.PopperElement
import frontend.components.utils.tailwind._
import frontend.components.utils.tailwind.forms._
import models.users.{NewUser, RouteDefinitions}
import models.validators.FieldsValidator
import org.scalajs.dom.html
import org.scalajs.dom.html.Form
import programs.frontend.login._
import services.http.FHttpClient
import services.routing._
import utils.ziohelpers._
import zio.{UIO, ZIO}

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
    div(
      className := "progress",
      width := "200px",
      className := "bg-gray-300 rounded h-2",
      div(
        //className := "progress-bar",
        className := "h-full rounded",
        role := "progressbar",
        width <-- $passwordStrengths.map(_ * 100).map(_.toString + "%"),
        className := "transition-all duration-1000 ease-in-out transform",
        className <-- $passwordStrengths.map {
          case x if x <= 0.3 => "bg-red-500"
          case x if x <= 0.6 => "bg-orange-500"
          case _             => "bg-green-500"
        }
      )
    ),
    span(
      "What is this?",
      dataAttr("container") := "body",
      title := "",
      dataAttr("content") := "This bar attempts to reflects the strength of your password. There is no need to panic if it is not fully green!",
      dataAttr("origin-title") := "Password strength",
      PopperElement.attachPopover
    )
  )

  val program: ZIO[NewUser, Nothing, Either[ErrorADT, Int]] = (for {
    newUser <- ZIO.environment[NewUser]
    statusCode <- register(newUser, validator).provideLayer(FHttpClient.live)
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
        formLabel("Username"),
        formInput(
          "text",
          placeholder := "Choose user name",
          inContext(elem => onChange.mapTo(elem.ref.value) --> nameChanger)
        )
      ),
      div(
        formGroup,
        formLabel("Password"),
        formInput(
          "password",
          placeholder := "Choose password",
          inContext(elem => onInput.mapTo(elem.ref.value) --> passwordChanger)
        )
      ),
      div(
        formGroup,
        div(className := "md:w-1/3"),
        div(className := "md:w-2/3", passwordStrengthBar)
      ),
      div(
        formGroup,
        formLabel("Confirm password"),
        formInput(
          "password",
          placeholder := "Confirm password",
          inContext(elem => onChange.mapTo(elem.ref.value) --> confirmPasswordChanger)
        )
      ),
      div(
        formGroup,
        formLabel("Email address"),
        formInput(
          "email",
          placeholder := "Enter your email",
          inContext(elem => onChange.mapTo(elem.ref.value) --> emailChanger)
        )
      ),
      div(
        formGroup,
        div(className := "md:w-1/3"),
        div(
          className := "md:w-2/3",
          input(`type` := "submit", value := "Sign-up", btn, primaryButton, cursorPointer, disabled <-- $isSubmitting)
        )
      )
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
