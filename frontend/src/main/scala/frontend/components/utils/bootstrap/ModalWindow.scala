package frontend.components.utils.bootstrap

import org.scalajs.dom.html
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.{BootstrapCSS, Component}

final class ModalWindow private (
    title: String,
    body: ReactiveHtmlElement[html.Paragraph],
    confirmButtonText: String,
    confirmButtonClick: ReactiveHtmlElement[html.Button] => Modifier[ReactiveHtmlElement[html.Button]]
) extends Component[html.Div] {

  import ModalWindow.closeModalBusWriter

  val element: ReactiveHtmlElement[html.Div] =
    div(
      className := "modal",
      div(
        className := "modal-dialog",
        role := "document",
        div(
          className := "modal-content",
          div(
            className := "modal-header",
            h5(className := "modal-title", title),
            button(
              `type` := "button",
              className := "close",
              dataAttr("dismiss") := "modal",
              aria.label := "Close",
              span(aria.hidden := true, "x"),
              onClick.mapTo(()) --> closeModalBusWriter
            )
          ),
          div(
            className := "modal-body",
            body
          ),
          div(
            className := "modal-footer",
            button(
              `type` := "button",
              BootstrapCSS.btnPrimary,
              confirmButtonText,
              inContext(confirmButtonClick)
            ),
            button(
              `type` := "button",
              BootstrapCSS.btnSecondary,
              dataAttr("dismiss") := "modal",
              "Close",
              onClick.mapTo(()) --> closeModalBusWriter
            )
          )
        )
      )
    )

}

object ModalWindow {
  private def apply(
      title: String,
      body: ReactiveHtmlElement[html.Paragraph],
      confirmButtonText: String,
      confirmButtonClick: ReactiveHtmlElement[html.Button] => Modifier[ReactiveHtmlElement[html.Button]]
  ) = new ModalWindow(title, body, confirmButtonText, confirmButtonClick)

  private val modalBus: EventBus[Option[
    (
        String,
        ReactiveHtmlElement[html.Paragraph],
        String,
        ReactiveHtmlElement[html.Button] => Modifier[ReactiveHtmlElement[html.Button]]
    )
  ]] = new EventBus

  val showModalBusWriter: Observer[
    (
        String,
        ReactiveHtmlElement[html.Paragraph],
        String,
        ReactiveHtmlElement[html.Button] => Modifier[ReactiveHtmlElement[html.Button]]
    )
  ] = modalBus.writer.contramap(Some(_))

  val closeModalBusWriter: Observer[Unit] = modalBus.writer.contramap(_ => None)

  val modalContainer: ReactiveHtmlElement[html.Div] = div(
    position := "absolute",
    top := "0px",
    left := "0px",
    width := "300px",
    height := "200px",
    zIndex := "100",
    className := "bs-component",
    child <-- modalBus.events.startWith(None).map(_.map((apply _).tupled)).map {
      case Some(modal) => modal
      case None        => emptyNode
    }
  )

  /** Opens the modal window with the given contents. */
  def showModal(
      title: String,
      body: ReactiveHtmlElement[html.Paragraph],
      confirmButtonText: String,
      confirmButtonClick: ReactiveHtmlElement[html.Button] => Modifier[ReactiveHtmlElement[html.Button]]
  ): Unit = showModalBusWriter.onNext(
    (title, body, confirmButtonText, confirmButtonClick)
  )

  /** Closes the modal window. */
  def closeModal(): Unit = closeModalBusWriter.onNext(())

}
