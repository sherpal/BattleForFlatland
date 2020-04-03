package frontend.components

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.connected.home.Home
import frontend.components.login.OutsideConnContainer
import frontend.components.utils.Redirect
import frontend.router.{Route, Routes}
import models.users.RouteDefinitions._
import org.scalajs.dom
import org.scalajs.dom.html.Div

final class App private () extends Component[dom.html.Div] {

  val element: ReactiveHtmlElement[Div] = div(
    child <-- Routes
      .firstOf(
        Route(entry, () => Redirect(homeRoute)),
        Route(loginRoute, () => OutsideConnContainer("Login")),
        Route(registerRoute, () => OutsideConnContainer("Sign-up")),
        Route(postRegisterRoute, (_: Unit, _: String) => OutsideConnContainer("Sign-up complete!")),
        Route(confirmRoute, (_: Unit, _: String) => OutsideConnContainer("Registering completed")),
        Route(homeRoute, () => Home())
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
