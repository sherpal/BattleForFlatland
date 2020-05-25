package frontend.components.utils

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import frontend.components.utils.ToggleButton.ToggleWriter
import org.scalajs.dom.html

final class ToggleButton private (toggleWriter: ToggleWriter, start: Boolean) extends Component[html.Span] {

  val internalToggleBus: EventBus[Unit] = new EventBus

  val $internalToggles: Signal[Boolean] = internalToggleBus.events.fold(start) {
    case (current, _) => !current
  }

  val $colour: Signal[String] = $internalToggles.map(if (_) "green-500" else "red-500")

  val element: ReactiveHtmlElement[html.Span] =
    span(
      className := "rounded-full w-8 h-4 flex",
      className <-- $internalToggles.map(if (_) "justify-end" else "justify-start"),
      className <-- $colour.map("bg-" + _),
      span(
        className := "rounded-full w-4 h-4 bg-white border-2",
        className <-- $colour.map("border-" + _)
      ),
      onClick.mapTo(()) --> internalToggleBus,
      $internalToggles --> toggleWriter
    )

}

object ToggleButton {
  def apply(toggleWriter: ToggleWriter, start: Boolean = false) = new ToggleButton(toggleWriter, start)

  type ToggleWriter = Observer[Boolean]
}
