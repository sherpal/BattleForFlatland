package frontend.components.login

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import frontend.router.{Link, RouteDefinitions}
import org.scalajs.dom
import org.scalajs.dom.html.Div

final class Login private () extends Component[dom.html.Div] {

  val element: ReactiveHtmlElement[Div] = div(
    className := "Login",
    LoginForm(),
    p(
      "Not account, yet? Sign-up ",
      span(className := "clickable", Link(RouteDefinitions.registerRoute)("here")),
      "!"
    )
  )

}

object Login {
  def apply(): Login = new Login
}
