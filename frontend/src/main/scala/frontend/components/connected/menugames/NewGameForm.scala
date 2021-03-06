package frontend.components.connected.menugames

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import org.scalajs.dom.html

final class NewGameForm private () extends Component[html.Form] {
  val element: ReactiveHtmlElement[html.Form] = form(
    )
}

object NewGameForm {
  def apply() = new NewGameForm
}
