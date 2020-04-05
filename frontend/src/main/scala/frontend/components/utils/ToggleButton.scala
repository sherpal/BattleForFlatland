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

  val element: ReactiveHtmlElement[html.Span] = {
    implicit val s: ReactiveHtmlElement[html.Span] = span(
      className := "rounded-full w-8 h-4 flex",
      className <-- $internalToggles.map(if (_) "justify-start" else "justify-end"),
      className <-- $internalToggles.map(if (_) "bg-green-500" else "bg-red-500"),
      span(
        className := "rounded-full w-4 h-4 bg-white border-2 border-gray-200"
      ),
      onClick.mapTo(()) --> internalToggleBus
    )

    $internalToggles.foreach(toggleWriter.onNext)

    s
  }

}

object ToggleButton {
  def apply(toggleWriter: ToggleWriter, start: Boolean = false) = new ToggleButton(toggleWriter, start)

  type ToggleWriter = Observer[Boolean]
}
