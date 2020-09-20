package frontend.components.connected.home

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import errors.ErrorADT
import frontend.components.Component
import frontend.components.connected.fixed.DashboardHeader
import frontend.components.connected.menugames.Games
import frontend.components.connected.menugames.gamejoined.GameJoined
import frontend.router.{Route, Routes}
import models.bff.Routes._
import models.users.Role.SuperUser
import models.users.RouteDefinitions._
import models.users.{RouteDefinitions, User}
import org.scalajs.dom.html
import programs.frontend.login._
import services.logging.log
import services.routing._
import utils.laminarzio.Implicits._
import zio.UIO

final class Home private () extends Component[html.Div] {

  val $me: EventStream[Either[ErrorADT, User]] = EventStream.fromZIOEffect(me.either)
  val $user: EventStream[User]                 = $me.collect { case Right(user) => user }
  val $amISuperUper: EventStream[Boolean]      = $user.map(_.roles.contains(SuperUser))
  val $doAsSuperUser: EventStream[Boolean]     = $amISuperUper.filter(identity)

  val $users: EventStream[List[User]] = $doAsSuperUser.flatMap(
    _ => EventStream.fromZIOEffect(users(0, 10).catchAll(error => log.error(error.toString) *> UIO(List[User]())))
  )

  val $redirect: EventStream[Unit] = $me.collect { case Left(error) => error }
    .filter(_.httpErrorType == errors.HTTPResultType.Unauthorized)
    .flatMap(_ => moveTo(RouteDefinitions.loginRoute))

  val element: ReactiveHtmlElement[html.Div] = div(
    child <-- $redirect.map(_ => "Not logged, redirecting to Login."), // kicking off stream
    className := "main-conn",
    DashboardHeader($user.map(_.userName)),
    child <-- $user.map { user =>
      div(
        className := "main",
        child <-- Routes
          .firstOf(
            Route(homeRoute, () => Games()),
            Route(gameJoined ? gameIdParam, (_: Unit, gameId: String) => GameJoined(gameId, user))
          )
          .map {
            case Some(component) => component
            case None            => div("uh-oh")
          }
        //child <-- $users.map(UserList(_))
      )
    }
  )
}

object Home {
  def apply() = new Home
}
