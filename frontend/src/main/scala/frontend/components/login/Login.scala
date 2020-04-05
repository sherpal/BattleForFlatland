package frontend.components.login

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import frontend.components.utils.tailwind._
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
      span(
        className := s"text-$primaryColour-$primaryColourLight hover:text-$primaryColour-$primaryColourDark",
        cursorPointer,
        Link(RouteDefinitions.registerRoute)("here")
      ),
      "!"
    )
  )

}

object Login {
  def apply(): Login = new Login
}
