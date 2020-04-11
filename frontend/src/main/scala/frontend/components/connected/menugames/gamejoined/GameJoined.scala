package frontend.components.connected.menugames.gamejoined

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.LifecycleComponent
import io.circe.generic.auto._
import io.circe.syntax._
import models.bff.Routes._
import models.bff.gameantichamber.WebSocketProtocol
import models.bff.outofgame.MenuGameWithPlayers
import models.users.RouteDefinitions
import org.scalajs.dom.html
import programs.frontend.games._
import services.http.FHttpClient
import services.logging.FLogging
import services.routing.FRouting
import utils.laminarzio.Implicits._
import utils.websocket.JsonWebSocket
import services.routing._
import zio.clock.Clock
import frontend.components.utils.tailwind._
import scalajs.js.timers.{clearInterval, setInterval}

import scala.scalajs.js
import scala.concurrent.duration._

final class GameJoined private (gameId: String) extends LifecycleComponent[html.Element] {

  private val layer = FHttpClient.live ++ FRouting.live ++ FLogging.live ++ Clock.live

  private val socket = JsonWebSocket[WebSocketProtocol, WebSocketProtocol, String](gameJoinedWS, gameIdParam, gameId)

  private val fetchingGameInfo = fetchGameInfo(gameId).provideLayer(layer)

  private val pokingHandle = setInterval(10.seconds) {
    zio.Runtime.default.unsafeRunToFuture(pokingPresence(gameId).provideLayer(layer))
  }

  val $gameInfo: EventStream[MenuGameWithPlayers] =
    socket.$in.collect { case WebSocketProtocol.GameStatusUpdated => () }
      .debounce(1000)
      .flatMap(_ => EventStream.fromZIOEffect(fetchingGameInfo))
      .collect { case Some(info) => info }

  val $firstGameInfo: EventStream[MenuGameWithPlayers] =
    EventStream.fromZIOEffect(fetchingGameInfo).collect { case Some(info) => info }

  val $gameCancelled: EventStream[Unit] = socket.$in.collect { case WebSocketProtocol.GameCancelled => () }
    .flatMap(_ => EventStream.fromZIOEffect(moveTo(RouteDefinitions.homeRoute).provideLayer(layer)))

  val cancelGameBus = new EventBus[Unit]
  val $cancelGame: EventStream[Int] =
    cancelGameBus.events.flatMap(_ => EventStream.fromZIOEffect(sendCancelGame(gameId).provideLayer(layer)))

  val elem: ReactiveHtmlElement[html.Element] = section(
    className <-- $gameCancelled.mapTo(""), // kicking off stream
    className <-- $cancelGame.mapTo(""), // kicking off stream
    p(
      s"You've joined game $gameId."
    ),
    pre(
      child.text <-- socket.$in.filterNot(_ == WebSocketProtocol.HeartBeat)
        .map(x => new js.Date().getTime.toString -> x.asJson.spaces2)
        .map(_.toString)
    ),
    pre(
      child.text <-- EventStream.merge($gameInfo, $firstGameInfo).map(_.asJson.spaces2)
    ),
    div(
      button(btn, primaryButton, "Cancel game", onClick.mapTo(()) --> cancelGameBus)
    )
  )

  override def componentDidMount(): Unit =
    socket.open()(elem)

  override def componentWillUnmount(): Unit = {
    socket.close()
    clearInterval(pokingHandle)
  }

}

object GameJoined {
  def apply(gameId: String) = new GameJoined(gameId)
}
