package frontend.components.login

import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import org.scalajs.dom.html
import org.scalajs.dom.html.Div
import com.raquo.laminar.api.L._

final class Register private () extends Component[html.Div] {
  override val element: ReactiveHtmlElement[Div] = div(
    className := "Register",
    h1("Register")
  )
}

object Register {
  def apply(): Register = new Register
}
