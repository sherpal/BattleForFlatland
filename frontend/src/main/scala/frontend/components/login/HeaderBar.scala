package frontend.components.login

import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import org.scalajs.dom.html
import com.raquo.laminar.api.L._
import frontend.router.Link
import models.users.RouteDefinitions._

final class HeaderBar private (title: String) extends Component[html.Element] {

  val login  = "Login"
  val signUp = "Sign-up"

  def active(name: String): Modifier[HtmlElement] = className := (if (name == title) "active" else "")

  val element: ReactiveHtmlElement[html.Element] =
    nav(
      className := "navbar navbar-expand-lg navbar-dark bg-primary",
      a(className := "navbar-brand", href := "#", "Battle for Flatland"),
      ul(
        className := "navbar-nav mr-auto",
        li(
          active(login),
          className := "nav-item",
          span(className := "nav-link", Link(loginRoute)(login))
        ),
        li(
          active(signUp),
          className := "nav-item",
          span(className := "nav-link", Link(registerRoute)(signUp))
        )
      )
    )

}

object HeaderBar {
  def apply(title: String) = new HeaderBar(title)
}
