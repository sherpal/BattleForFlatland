package frontend.components.connected.menugames

import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import org.scalajs.dom.html
import com.raquo.laminar.api.L._
import models.bff.outofgame.MenuGameWithPlayers
import programs.frontend.games._
import services.http.FHttpClient
import services.logging.FLogging
import services.routing.FRouting
import utils.laminarzio.Implicits._
import io.circe.syntax._
import io.circe.generic.auto._

final class GameJoined private (gameId: String) extends Component[html.Element] {

  private val layer = FHttpClient.live ++ FRouting.live ++ FLogging.live

  val $gameInfo: EventStream[MenuGameWithPlayers] =
    EventStream.fromZIOEffect(fetchGameInfo(gameId).provideLayer(layer)).collect { case Some(info) => info }

  val element: ReactiveHtmlElement[html.Element] = section(
    p(
      s"You've joined game $gameId."
    ),
    pre(
      child.text <-- $gameInfo.map(_.asJson.spaces2)
    )
  )

}

object GameJoined {
  def apply(gameId: String) = new GameJoined(gameId)
}
