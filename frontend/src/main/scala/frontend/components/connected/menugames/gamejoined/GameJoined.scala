package frontend.components.connected.menugames.gamejoined

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.LifecycleComponent
import io.circe.generic.auto._
import io.circe.syntax._
import models.bff.Routes._
import models.bff.gameantichamber.WebSocketProtocol
import models.bff.outofgame.MenuGameWithPlayers
import models.users.{RouteDefinitions, User}
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

import scalajs.js.timers.{clearInterval, setInterval, SetIntervalHandle}
import scala.scalajs.js
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

final class GameJoined private (gameId: String, me: User) extends LifecycleComponent[html.Element] {

  private val layer = FHttpClient.live ++ FRouting.live ++ FLogging.live ++ Clock.live

  private val socket = JsonWebSocket[WebSocketProtocol, WebSocketProtocol, String](gameJoinedWS, gameIdParam, gameId)

  private val fetchingGameInfo = fetchGameInfo(gameId).provideLayer(layer)

  private val pokingHandle: SetIntervalHandle = setInterval(10.seconds) {
    zio.Runtime.default.unsafeRunToFuture(pokingPresence(gameId).provideLayer(layer)) onComplete {
      case Success(_) =>
      case Failure(_) => clearInterval(pokingHandle)
    }
  }

  val $gameInfo: EventStream[MenuGameWithPlayers] =
    EventStream
      .merge(
        socket.$in.collect { case WebSocketProtocol.GameStatusUpdated => () }
          .debounce(1000)
          .flatMap(_ => EventStream.fromZIOEffect(fetchingGameInfo)),
        EventStream.fromZIOEffect(fetchingGameInfo)
      )
      .collect { case Some(info) => info }

  val $amICreator: EventStream[Boolean] = $gameInfo.map(_.game.gameCreator.userId == me.userId)

  val $gameCancelled: EventStream[Unit] = socket.$in.collect { case WebSocketProtocol.GameCancelled => () }

  val $shouldMoveBackToHome: EventStream[Unit] = EventStream
    .merge(
      $gameCancelled,
      socket.$closed,
      socket.$error.mapTo(())
    )
    .flatMap(_ => EventStream.fromZIOEffect(moveTo(RouteDefinitions.homeRoute).provideLayer(layer)))

  val cancelGameBus = new EventBus[Unit]
  val $cancelGame: EventStream[Int] =
    cancelGameBus.events.flatMap(_ => EventStream.fromZIOEffect(sendCancelGame(gameId).provideLayer(layer)))

  val leaveGameBus = new EventBus[Unit]
  val $leaveGame: EventStream[Int] =
    leaveGameBus.events.flatMap(_ => EventStream.fromZIOEffect(iAmLeaving(gameId).provideLayer(layer)))

  val elem: ReactiveHtmlElement[html.Element] = section(
    mainContentContainer,
    className <-- $shouldMoveBackToHome.mapTo(""), // kicking off stream
    className <-- $cancelGame.mapTo(""), // kicking off stream
    className <-- $leaveGame.mapTo(""), // kicking off stream
    div(
      mainContent,
      h1(
        className := "text-3xl",
        className := s"text-$primaryColour-$primaryColourDark",
        child.text <-- $gameInfo.map(_.game.gameName).map("Game " + _)
      ),
      PlayerList($gameInfo.map(_.players)),
//      p(
//        s"You've joined game $gameId."
//      ),
//      pre(
//        child.text <-- socket.$in.filterNot(_ == WebSocketProtocol.HeartBeat)
//          .map(x => new js.Date().getTime.toString -> x.asJson.spaces2)
//          .map(_.toString)
//      ),
//      pre(
//        child.text <-- $gameInfo.map(_.asJson.spaces2)
//      ),
      div(
        child <-- $amICreator.map {
          if (_)
            button(btn, primaryButton, "Cancel game", onClick.mapTo(()) --> cancelGameBus)
          else
            button(
              btn,
              primaryButton,
              "Leave game",
              onClick.mapTo(WebSocketProtocol.PlayerLeavesGame(me.userId)) --> socket.outWriter
            )
        }
      )
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
  def apply(gameId: String, me: User) = new GameJoined(gameId, me)
}
