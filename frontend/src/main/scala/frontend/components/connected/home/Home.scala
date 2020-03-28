package frontend.components.connected.home

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import errors.ErrorADT
import frontend.components.Component
import frontend.components.connected.fixed.DashboardHeader
import models.users.Role.SuperUser
import models.users.{RouteDefinitions, User}
import org.scalajs.dom.html
import utils.laminarzio.Implicits._
import programs.frontend.login.{amISuperUser, me}
import services.http.FrontendHttpClient.{live => httpLive}
import services.routing._

final class Home private () extends Component[html.Div] {

  val $me: EventStream[Either[ErrorADT, User]] = EventStream.fromZIOEffect(me.either.provideLayer(httpLive))
  val $user: EventStream[User]                 = $me.collect { case Right(user) => user }
  val $redirect: EventStream[Unit] = $me.filter(_.isLeft).flatMap(
    _ => {
      EventStream.fromZIOEffect(
        moveTo(RouteDefinitions.loginRoute).provideLayer(FRouting.live)
      )
    }
  )
  val $amISuperUper: EventStream[Boolean] = $user.map(_.roles.contains(SuperUser))

  val element: ReactiveHtmlElement[html.Div] = div(
    className := "main-conn",
    DashboardHeader($user.map(_.userName)),
    div(
      child <-- $redirect.map(_ => "Not logged, redirecting to Login."),
      className := "main",
      h1("Battle for Flatland"),
      p(s"Hello, ", child <-- $user.map(_.userName)),
      p(
        child <-- $amISuperUper.map {
          if (_) "You are a SuperUser."
          else "You are not a SuperUser."
        }
      )
    )
  )
}

object Home {
  def apply() = new Home
}
