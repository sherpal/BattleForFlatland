package frontend.components.connected.menugames

import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import org.scalajs.dom.html
import com.raquo.laminar.api.L._
import com.raquo.laminar.lifecycle.{NodeDidMount, NodeWasDiscarded, NodeWillUnmount}
import programs.frontend.games.streamExpl
import utils.laminarzio.Implicits._
import zio.clock.Clock

final class NewGame private (closeWriter: Observer[Unit]) extends Component[html.Div] {

  private val layer = Clock.live

  val (f, xs) = EventStream
    .fromZStream(streamExpl.provideLayer(layer))

  val element: ReactiveHtmlElement[html.Div] = {
    val d = div(
      className := "NewGame",
      zIndex := 5,
      position := "fixed",
      top := "0px",
      left := "0px",
      p("new game works"),
      NewGameForm(),
      button("Cancel", onClick.mapTo(()) --> closeWriter),
      child <-- xs
        .map(e => {
          println(e)
          e
        })
        .map(_.toString)
    )

    d.subscribe(_.mountEvents) {
      case NodeDidMount =>
      case NodeWillUnmount =>
        f.cancel()
      case NodeWasDiscarded =>
    }

    d
  }

}

object NewGame {
  def apply(closeWriter: Observer[Unit]) = new NewGame(closeWriter)
}
