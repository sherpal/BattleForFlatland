package frontend.components

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.connected.home.Home
import frontend.components.connected.ingame.GamePlayingContainer
import frontend.components.login.OutsideConnContainer
import frontend.components.test.Test
import frontend.components.utils.Redirect
import frontend.router.{Route, Routes}
import models.bff.Routes._
import models.users.RouteDefinitions._
import org.scalajs.dom
import org.scalajs.dom.html.Div

final class App private () extends Component[dom.html.Div] {

  val element: ReactiveHtmlElement[Div] = div(
    className := "App",
    child <-- Routes
      .firstOf(
        Route(entry, () => Redirect(homeRoute)),
        Route(loginRoute, () => OutsideConnContainer("Login")),
        Route(registerRoute, () => OutsideConnContainer("Sign-up")),
        Route(postRegisterRoute, (_: Unit, _: String) => OutsideConnContainer("Sign-up complete!")),
        Route(confirmRoute, (_: Unit, _: String) => OutsideConnContainer("Registering completed")),
        Route(homeRoute, () => Home()),
        Route(gameJoined ? gameIdParam, (_: Unit, _: String) => Home()),
        Route(
          inGame ? (gameIdParam & tokenParam), { (_: Unit, gameIdAndToken: (String, String)) =>
            GamePlayingContainer(gameIdAndToken._1, gameIdAndToken._2)
          }
        ),
        Route(testRoute, () => Test())
      )
      .map {
        case Some(elem) => elem
        case None       => div("uh oh") // todo: 404
      }
  )

}

object App {
  def apply(): App = new App
}
