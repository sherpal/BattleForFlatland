package frontend.components.login

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.{BootstrapCSS, Component}
import frontend.router.Link
import models.users.RouteDefinitions
import org.scalajs.dom
import org.scalajs.dom.html

final class Login private () extends Component[dom.html.Element] {

  val element: ReactiveHtmlElement[html.Element] = section(
    className := "Login",
    LoginForm(),
    p(
      "Not account, yet? Sign-up ",
      span(BootstrapCSS.textInfo, Link(RouteDefinitions.registerRoute)("here")),
      "!"
    )
  )

}

object Login {
  def apply(): Login = new Login
}
