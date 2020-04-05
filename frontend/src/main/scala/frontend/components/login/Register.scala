package frontend.components.login

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import org.scalajs.dom.html

final class Register private () extends Component[html.Element] {
  override val element: ReactiveHtmlElement[html.Element] = section(
    className := "Register",
    RegisterForm()
  )
}

object Register {
  def apply(): Register = new Register
}
