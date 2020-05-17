package frontend.components.utils

import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import org.scalajs.dom.html
import org.scalajs.dom.html.Element
import com.raquo.laminar.api.L._
import frontend.components.utils.tailwind._

final class ColourPicker private () extends Component[html.Element] {
  val element: ReactiveHtmlElement[Element] = aside(
    modal.modalContainer,
    className := "flex",
    h1(textPrimaryColour, "Colour Picker")
  )
}
