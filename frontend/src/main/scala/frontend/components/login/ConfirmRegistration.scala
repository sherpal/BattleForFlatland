package frontend.components.login

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import errors.ErrorADT
import frontend.components.Component
import frontend.components.utils.PrimaryLink
import frontend.router.{Link, RouteDefinitions}
import org.scalajs.dom.html
import org.scalajs.dom.html.Div
import programs.frontend.login.confirmRegistrationCall
import services.http._
import utils.laminarzio.Implicits._

final class ConfirmRegistration private (registrationKey: String) extends Component[html.Div] {

  val $confirmRegistration: EventStream[Either[ErrorADT, Int]] =
    EventStream.fromZIOEffect(confirmRegistrationCall(registrationKey).provideLayer(FrontendHttpClient.live))

  val $confirmSuccess: EventStream[Int]      = $confirmRegistration.collect { case Right(code) => code }
  val $confirmFailure: EventStream[ErrorADT] = $confirmRegistration.collect { case Left(error) => error }

  val element: ReactiveHtmlElement[Div] = div(
    className := "ConfirmRegistration",
    p(
      "Registration key: ",
      registrationKey
    ),
    child <-- $confirmSuccess.map { _ =>
      div(
        h2("Registration succeeded!"),
        p(
          "You can now connect to Battle for flatland.",
          br(),
          PrimaryLink(RouteDefinitions.loginRoute)("Login")
        )
      )
    },
    child <-- $confirmFailure.map { error =>
      div(
        h2("Fatal error"),
        p(s"Error code: $error")
      )
    }
  )

}

object ConfirmRegistration {
  def apply(registrationKey: String): ConfirmRegistration = new ConfirmRegistration(registrationKey)

}
