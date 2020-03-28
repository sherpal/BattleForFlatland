package frontend.components.login

import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import org.scalajs.dom.html
import org.scalajs.dom.html.Div
import com.raquo.laminar.api.L._
import frontend.router.{Link, RouteDefinitions}

final class PostRegister private (userName: String) extends Component[html.Div] {
  val element: ReactiveHtmlElement[Div] = div(
    h1("Sign-up successful!"),
    p(s"Thank you, $userName, for registering to Battle For Flatland!"),
    p("You should soon receive an email with a confirmation link to follow in order to confirm you registration."),
    Link(RouteDefinitions.loginRoute)("Login")
  )
}

object PostRegister {
  def apply(userName: String): PostRegister = new PostRegister(userName)
}
