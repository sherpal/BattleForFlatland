package frontend.components.connected.menugames

import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import org.scalajs.dom.html
import com.raquo.laminar.api.L._

final class GameJoined private (gameId: String) extends Component[html.Element] {

  val element: ReactiveHtmlElement[html.Element] = section(
    p(
      s"You've joined game $gameId."
    )
  )

}

object GameJoined {
  def apply(gameId: String) = new GameJoined(gameId)
}
