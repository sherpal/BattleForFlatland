package frontend.components

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.login.{ConfirmRegistration, Login, Register}
import frontend.router.RouteDefinitions._
import frontend.router.{Route, Routes}
import org.scalajs.dom
import org.scalajs.dom.html.Div

final class App private () extends Component[dom.html.Div] {

  val element: ReactiveHtmlElement[Div] = div(
    child <-- Routes
      .firstOf(
        Route(loginRoute, () => Login()),
        Route(registerRoute, () => Register()),
        Route(confirmRoute, (_: Unit, key: String) => ConfirmRegistration(key))
      )
      .map {
        case Some(elem) => elem
        case None       => div("uh oh")
      }
  )

}

object App {
  def apply(): App = new App
}
