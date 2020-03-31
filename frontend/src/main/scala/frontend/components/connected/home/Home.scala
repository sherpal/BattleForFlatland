package frontend.components.connected.home

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import errors.ErrorADT
import frontend.components.Component
import frontend.components.connected.fixed.DashboardHeader
import frontend.components.connected.menugames.Games
import models.users.Role.SuperUser
import models.users.{RouteDefinitions, User}
import org.scalajs.dom.html
import programs.frontend.login._
import services.http.FrontendHttpClient.{live => httpLive}
import services.http.HttpClient
import services.routing._
import utils.laminarzio.Implicits._
import services.logging.{log, FLogging, Logging}
import zio.{UIO, ZLayer}

final class Home private () extends Component[html.Div] {

  private val layer = httpLive ++ FLogging.live ++ FRouting.live
  //.asInstanceOf[ZLayer[Any, Nothing, HttpClient with Logging with Routing]] // Intellij...

  val $me: EventStream[Either[ErrorADT, User]] = EventStream.fromZIOEffect(me.either.provideLayer(layer))
  val $user: EventStream[User]                 = $me.collect { case Right(user) => user }
  val $amISuperUper: EventStream[Boolean]      = $user.map(_.roles.contains(SuperUser))
  val $doAsSuperUser: EventStream[Boolean]     = $amISuperUper.filter(identity)

  val $users: EventStream[List[User]] = $doAsSuperUser.flatMap(
    _ =>
      EventStream.fromZIOEffect(
        users(0, 10)
          .catchAll(error => log.error(error.toString) *> UIO(List[User]()))
          .provideLayer(layer)
      )
  )

  val $redirect: EventStream[Unit] = $me.filter(_.isLeft).flatMap(
    _ => {
      EventStream.fromZIOEffect(
        moveTo(RouteDefinitions.loginRoute).provideLayer(layer)
      )
    }
  )

  val element: ReactiveHtmlElement[html.Div] = div(
    className := "main-conn",
    DashboardHeader($user.map(_.userName)),
    div(
      child <-- $redirect.map(_ => "Not logged, redirecting to Login."), // kicking off stream
      className := "main",
      h1("Battle for Flatland"),
      p(s"Hello, ", child <-- $user.map(_.userName)),
      p(
        child <-- $amISuperUper.map {
          if (_) "You are a SuperUser."
          else "You are not a SuperUser."
        }
      ),
      Games(),
      child <-- $users.map(UserList(_))
    )
  )
}

object Home {
  def apply() = new Home
}
