package frontend.components.connected.menugames

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import org.scalajs.dom.html

final class Games private () extends Component[html.Div] {

  val showNewGameBus: EventBus[Boolean] = new EventBus
  val closeWriter: Observer[Unit]       = showNewGameBus.writer.contramap[Unit](_ => false)
  val showNewGame$ : Signal[ReactiveHtmlElement[html.Div]] = showNewGameBus.events.startWith(false).map {
    if (_) NewGame(closeWriter)
    else div()
  }

  val element: ReactiveHtmlElement[html.Div] = div(
    className := CSS.games.name,
    button("New game", onClick.mapTo(true) --> showNewGameBus),
    child <-- showNewGame$
  )
}

object Games {
  def apply() = new Games
}
