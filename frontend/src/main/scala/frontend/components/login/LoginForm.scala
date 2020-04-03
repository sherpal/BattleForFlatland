package frontend.components.login

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import errors.ErrorADT
import errors.ErrorADT.{ErrorOr, IncorrectPassword}
import frontend.components.Component
import frontend.components.forms.SimpleForm
import models.users.{LoginUser, RouteDefinitions}
import models.validators.FieldsValidator
import org.scalajs.dom.html
import org.scalajs.dom.html.Form
import programs.frontend.login.login
import services.http.FrontendHttpClient
import services.routing._
import utils.ziohelpers._
import zio.{UIO, URIO, ZIO}
import frontend.components.BootstrapCSS._

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
  val touch                       = onFocus.mapTo(false) --> $touched

  val $isInvalid =
    EventStream
      .merge(
        $touched.events,
        $invalidPassword
      )
      .map(if (_) "is-invalid" else "")

  def submitProgram(loginUser: LoginUser): UIO[ErrorOr[Int]] = program.provide(loginUser)

  implicit val element: ReactiveHtmlElement[Form] = form(
    submit,
    fieldSet(
      div(
        formGroup,
        label("User name"),
        input(
          `type` := "text",
          formControl,
          className <-- $isInvalid,
          placeholder := "User name",
          inContext(elem => onChange.mapTo(elem.ref.value) --> nameChanger),
          touch
        )
      ),
      div(
        formGroup,
        label("Password"),
        input(
          `type` := "password",
          formControl,
          className <-- $isInvalid,
          placeholder := "enter password...",
          inContext(elem => onChange.mapTo(elem.ref.value) --> passwordChanger),
          touch
        )
      ),
      p(
        className := "text-danger",
        "Invalid username and/or password.",
        display <-- $invalidPassword.startWith(false).map(if (_) "" else "none")
      )
    ),
    input(
      `type` := "submit",
      value := "Login",
      disabled <-- $isSubmitting,
      btnPrimary
    )
  )

  $formData.foreach(println)

}

object LoginForm {
  def apply(): LoginForm = new LoginForm
}
