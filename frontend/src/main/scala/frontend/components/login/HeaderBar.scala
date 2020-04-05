package frontend.components.login

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import frontend.components.utils.tailwind._
import frontend.router.Link
import models.users.RouteDefinitions._
import org.scalajs.dom.html
import org.scalajs.dom.html.LI
import urldsl.language.PathSegment

final class HeaderBar private (title: String) extends Component[html.Element] {

  val login  = "Login"
  val signUp = "Sign-up"

  def active(name: String): Modifier[HtmlElement] = className := (if (name == title) "text-indigo-200" else "")

  def navItem(text: String, path: PathSegment[Unit, _]): ReactiveHtmlElement[LI] = li(
    pad(2),
    className := "hover:text-indigo-200",
    active(text),
    className := "cursor-pointer",
    Link(path)(text)
  )

  val element: ReactiveHtmlElement[html.Element] =
    nav(
      headerStyle,
      h1(className := "h-8", pad(2), "Battle for Flatland"),
      ul(
        className := "flex justify-between",
        pad(2),
        navItem(login, loginRoute),
        navItem(signUp, registerRoute)
      )
    )

}

object HeaderBar {
  def apply(title: String) = new HeaderBar(title)
}
