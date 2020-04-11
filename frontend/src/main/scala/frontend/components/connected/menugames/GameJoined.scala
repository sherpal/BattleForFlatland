package frontend.components.connected.menugames

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.{Component, LifecycleComponent}
import io.circe.generic.auto._
import io.circe.syntax._
import models.bff.gameantichamber.WebSocketProtocol
import models.bff.outofgame.MenuGameWithPlayers
import org.scalajs.dom.html
import programs.frontend.games._
import services.http.FHttpClient
import services.logging.FLogging
import services.routing.FRouting
import utils.laminarzio.Implicits._
import utils.websocket.JsonWebSocket
import models.bff.Routes._

import scala.scalajs.js

final class GameJoined private (gameId: String) extends LifecycleComponent[html.Element] {

  private val layer = FHttpClient.live ++ FRouting.live ++ FLogging.live

  private val socket = JsonWebSocket[WebSocketProtocol, WebSocketProtocol, String](gameJoinedWS, gameIdParam, gameId)

  val $gameInfo: EventStream[MenuGameWithPlayers] =
    EventStream.fromZIOEffect(fetchGameInfo(gameId).provideLayer(layer)).collect { case Some(info) => info }

  val elem: ReactiveHtmlElement[html.Element] = section(
    p(
      s"You've joined game $gameId."
    ),
    pre(
      child.text <-- socket.$in.map(x => new js.Date().getTime.toString -> x.asJson.spaces2).map(_.toString)
    ),
    pre(
      child.text <-- $gameInfo.map(_.asJson.spaces2)
    )
  )

  override def componentDidMount(): Unit =
    socket.open()(elem)

  override def componentWillUnmount(): Unit =
    socket.close()

}

object GameJoined {
  def apply(gameId: String) = new GameJoined(gameId)
}
