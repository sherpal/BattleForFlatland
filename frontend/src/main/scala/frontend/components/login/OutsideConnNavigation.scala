package frontend.components.login

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import frontend.components.utils.PrimaryLink
import frontend.router.Link
import frontend.router.RouteDefinitions._
import org.scalajs.dom.html

final class OutsideConnNavigation private () extends Component[html.Element] {

  val element: ReactiveHtmlElement[html.Element] = nav(
    span(className := "secondary", Link(loginRoute)("Login")),
    span(
      className := "primary",
      Link(registerRoute)("Sign-up")
    )
  )
}

object OutsideConnNavigation {
  def apply() = new OutsideConnNavigation
}
