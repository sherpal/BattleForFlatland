package frontend.components.test

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.LifecycleComponent
import org.scalajs.dom.html
import utils.websocket.JsonWebSocket
import urldsl.language.PathSegment.dummyErrorImpl._

final class WebSocketTest private () extends LifecycleComponent[html.Div] {

  private val socket = JsonWebSocket[String, String](root / "ws-test")

  implicit val elem: ReactiveHtmlElement[html.Div] = div(
    className := "bg-red-200",
    input(inContext(elem => onChange.mapTo(elem.ref.value) --> socket.outWriter)),
    child.text <-- socket.$in.map("Returned: " + _)
  )

  override def componentDidMount(): Unit = socket.open()

}

object WebSocketTest {
  def apply() = new WebSocketTest
}
