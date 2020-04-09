package frontend.components

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.ModalWindow.CloseWriter
import org.scalajs.dom.html

final class ModalWindow private (content: ReactiveHtmlElement[html.Element], closeWriter: CloseWriter)
    extends Component[html.Div] {

  val element: ReactiveHtmlElement[html.Div] = div(
    className := "flex flex-col items-center justify-center absolute inset-0 z-50",
    //div(className := "h-32 w-5"),
    div(
      onClick.stopPropagation --> (_ => {}),
      content
    ),
    onClick.mapTo(()) --> closeWriter
  )

}

object ModalWindow {
  def apply(content: ReactiveHtmlElement[html.Element], closeWriter: CloseWriter) =
    new ModalWindow(content, closeWriter)

  type CloseWriter = Observer[Unit]
}
