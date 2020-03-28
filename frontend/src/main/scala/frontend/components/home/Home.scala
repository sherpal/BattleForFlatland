package frontend.components.home

import frontend.components.Component
import org.scalajs.dom.html
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement

final class Home private () extends Component[html.Div] {
  val element: ReactiveHtmlElement[html.Div] = div(
    h1("Battle for Flatland")
  )
}

object Home {
  def apply() = new Home
}
