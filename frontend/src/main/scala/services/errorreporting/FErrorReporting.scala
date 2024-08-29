package services.errorreporting

import be.doeraene.webcomponents.ui5.*
import com.raquo.laminar.api.L.*
import org.scalajs.dom
import zio.ZIO
import be.doeraene.webcomponents.ui5.configkeys.IllustratedMessageType
import java.io.StringWriter
import java.io.PrintWriter
import be.doeraene.webcomponents.ui5.configkeys.MessageStripDesign

class FErrorReporting(container: String | dom.Element) extends ErrorReporting {

  private val root: dom.Element = container match {
    case containerId: String =>
      Option(dom.document.getElementById(containerId)).getOrElse {
        val elem = dom.document.createElement("div")
        dom.document.body.appendChild(elem)
        elem
      }
    case container: dom.Element => container
  }

  val errorBus = new EventBus[Throwable]
  val closeBus = new EventBus[Unit]

  private case class ErrorData(message: String, stackTrace: String)
  private def errorData(err: Throwable): ErrorData = ErrorData(
    Option(err.getMessage()).getOrElse("Unknown error, this is bad"), {
      val sw = new StringWriter
      err.printStackTrace(new PrintWriter(sw))
      sw.toString
    }
  )
  private val errorDataStream = errorBus.events.map(errorData)

  override def showError(err: Throwable): ZIO[Any, Nothing, Unit] =
    ZIO.succeed(errorBus.writer.onNext(err))

  render(
    root,
    Dialog(
      _.stretch := true,
      _.open   <-- EventStream.merge(closeBus.events.mapTo(false), errorBus.events.mapTo(true)),
      _.slots.header := Bar(_.slots.startContent := Label("Fatal error encountered!")),
      child <-- errorDataStream.map { case ErrorData(message, stackTrace) =>
        div(
          Text("An unhandled error occurred during the application."),
          MessageStrip(_.hideCloseButton := true, _.design := MessageStripDesign.Negative, message),
          pre(
            overflow.auto,
            stackTrace
          )
        )
      },
      _.slots.footer := Bar(
        _.slots.endContent := Button("Close", _.events.onClick.mapToUnit --> closeBus.writer)
      )
    )
  )

}
