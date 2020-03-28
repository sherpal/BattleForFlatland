package frontend.components.login

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import frontend.components.utils.PrimaryLink
import frontend.router.Link
import models.users.RouteDefinitions._
import org.scalajs.dom.html

final class OutsideConnNavigation private () extends Component[html.Element] {

  val element: ReactiveHtmlElement[html.Element] = nav(
    span(className := "secondary clickable", Link(loginRoute)("Login")),
    span(
      className := "primary clickable",
      Link(registerRoute)("Sign-up")
    )
  )
}

object OutsideConnNavigation {
  def apply() = new OutsideConnNavigation
}
