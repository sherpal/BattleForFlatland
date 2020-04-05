package frontend.components.login

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import errors.ErrorADT
import errors.ErrorADT.{ErrorOr, IncorrectPassword}
import frontend.components.Component
import frontend.components.forms.SimpleForm
import frontend.components.utils.tailwind._
import frontend.components.utils.tailwind.forms._
import models.users.{LoginUser, RouteDefinitions}
import models.validators.FieldsValidator
import org.scalajs.dom.html
import org.scalajs.dom.html.Form
import programs.frontend.login.login
import services.http.FrontendHttpClient
import services.routing._
import utils.ziohelpers._
import zio.{UIO, URIO, ZIO}

final class LoginForm private () extends Component[html.Form] with SimpleForm[LoginUser, ErrorOr[Int]] {

  val initialData: LoginUser                          = LoginUser.empty
  val validator: FieldsValidator[LoginUser, ErrorADT] = LoginUser.validator

  val nameChanger: Observer[String]     = makeDataChanger((name: String) => _.copy(userName = name))
  val passwordChanger: Observer[String] = makeDataChanger((pw: String) => _.copy(password   = pw))

  val program: URIO[LoginUser, Either[ErrorADT, Int]] = (for {
    loginUser <- ZIO.environment[LoginUser]
    statusCode <- login(loginUser).provideLayer(FrontendHttpClient.live)
    // should never fail as it should fail before.
    _ <- unsuccessfulStatusCode(statusCode)
    _ <- moveTo(RouteDefinitions.homeRoute).provideLayer(FRouting.live)
  } yield statusCode).either

  val $submitErrors: EventStream[Boolean] = $submitEvents.map(_.isLeft)

  val $invalidPassword: EventStream[Boolean] = $submitEvents.map {
    case Left(IncorrectPassword) => true
    case _                       => false
  }

  val $touched: EventBus[Boolean] = new EventBus()
  final val touch                 = onFocus.mapTo(false) --> $touched

  final val $isInvalid =
    EventStream
      .merge(
        $touched.events,
        $invalidPassword
      )
      .map(if (_) "is-invalid" else "")

  def submitProgram(loginUser: LoginUser): UIO[ErrorOr[Int]] = program.provide(loginUser)

  val element: ReactiveHtmlElement[Form] = form(
    submit,
    fieldSet(
      div(
        formGroup,
        formLabel("User name"),
        formInput(
          "text",
          className <-- $isInvalid,
          placeholder := "Enter user name...",
          inContext(elem => onInput.mapTo(elem.ref.value) --> nameChanger),
          touch
        )
      ),
      div(
        formGroup,
        formLabel("Password"),
        formInput(
          "password",
          className <-- $isInvalid,
          placeholder := "Enter password...",
          inContext(elem => onInput.mapTo(elem.ref.value) --> passwordChanger),
          touch
        )
      ),
      p(
        className := "text-danger",
        "Invalid username and/or password.",
        display <-- $invalidPassword.startWith(false).map(if (_) "" else "none")
      )
    ),
    div(
      formGroup,
      div(className := "md:w-1/3"),
      div(
        className := "wd-w-2/3 justify-between",
        input(
          `type` := "submit",
          value := "Login",
          disabled <-- $isSubmitting,
          btn,
          btnIndigo
        ),
        span(
          className := "text-indigo-500 hover:text-indigo-900 px-4",
          cursorPointer,
          "Forgot password?"
        )
      )
    )
  )

  init()
}

object LoginForm {
  def apply(): LoginForm = new LoginForm
}
