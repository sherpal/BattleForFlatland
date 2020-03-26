package frontend.components.login

import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import org.scalajs.dom
import org.scalajs.dom.html.Div
import com.raquo.laminar.api.L._

final class Login private () extends Component[dom.html.Div] {

  val element: ReactiveHtmlElement[Div] = div(
    className := "Login",
    h1("Login"),
    LoginForm()
  )

}

object Login {
  def apply(): Login = new Login
}
