package frontend.components.test

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import org.scalajs.dom.html
import urldsl.language.PathSegment.dummyErrorImpl._
import utils.websocket.JsonWebSocket

final class WebSocketTest private () extends Component[html.Div] {

  private val socket = JsonWebSocket[String, String](root / "ws-test")

  val element: ReactiveHtmlElement[html.Div] = div(
    className := "bg-red-200",
    input(inContext(elem => onChange.mapTo(elem.ref.value) --> socket.outWriter)),
    child.text <-- socket.$in.map("Returned: " + _),
    onMountCallback(ctx => socket.open()(ctx.owner))
  )

}

object WebSocketTest {
  def apply() = new WebSocketTest
}
