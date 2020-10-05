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
import services.http.HttpClient
import services.routing._
import services.toaster.{toast, Toaster}
import utils.laminarzio.Implicits._
import utils.ziohelpers._
import zio.{URIO, ZIO}
import services.toaster.ToasterModifierBuilder._

import scala.concurrent.duration.DurationInt

final class LoginForm private () extends Component[html.Form] with SimpleForm[LoginUser, ErrorOr[Int]] {

  val initialData: LoginUser                          = LoginUser.empty
  val validator: FieldsValidator[LoginUser, ErrorADT] = LoginUser.validator

  val nameChanger: Observer[String]     = makeDataChanger((name: String) => _.copy(userName = name))
  val passwordChanger: Observer[String] = makeDataChanger((pw: String) => _.copy(password   = pw))

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

  def submitProgram(loginUser: LoginUser): URIO[Toaster with Routing with HttpClient, Either[ErrorADT, Int]] =
    (for {
      statusCode <- login(loginUser)
      // should never fail as it should fail before.
      _ <- unsuccessfulStatusCode(statusCode)
      _ <- moveTo(RouteDefinitions.homeRoute)
      _ <- toast.success("Logged in!", autoCloseDuration := 2.seconds)
    } yield statusCode).either

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
      $invalidPassword.filter(identity)
        .flatMap(_ => toast.error("Invalid username and/or password")) --> Observer.empty
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
          primaryButton
        ),
        span(
          btn,
          secondaryButton,
          "Forgot password?"
        )
      )
    )
  )

}

object LoginForm {
  def apply(): LoginForm = new LoginForm
}
