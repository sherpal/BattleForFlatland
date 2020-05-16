package frontend.components.connected.menugames.gamejoined

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.LifecycleComponent
import frontend.components.utils.tailwind._
import io.circe.syntax._
import models.bff.Routes._
import models.bff.gameantichamber.WebSocketProtocol
import models.bff.ingame.GameUserCredentials
import models.bff.outofgame.MenuGameWithPlayers
import models.bff.outofgame.gameconfig.PlayerInfo
import models.users.{RouteDefinitions, User}
import org.scalajs.dom.html
import programs.frontend.games._
import services.http.FHttpClient
import services.logging.FLogging
import services.routing.{FRouting, _}
import utils.laminarzio.Implicits._
import utils.websocket.JsonWebSocket
import zio.clock.Clock

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.scalajs.js.timers.{clearInterval, setInterval, SetIntervalHandle}
import scala.util.{Failure, Success}

final class GameJoined private (gameId: String, me: User) extends LifecycleComponent[html.Element] {

  private val layer = FHttpClient.live ++ FRouting.live ++ FLogging.live ++ Clock.live

  private val socket = JsonWebSocket[WebSocketProtocol, WebSocketProtocol, String](gameJoinedWS, gameIdParam, gameId)

  private val fetchingGameInfo = fetchGameInfo(gameId).provideLayer(layer)

  /** Sends a message to the server proving that we are still connected to the game. */
  private val pokingHandle: SetIntervalHandle = setInterval(10.seconds) {
    zio.Runtime.default.unsafeRunToFuture(pokingPresence(gameId).provideLayer(layer)) onComplete {
      case Success(_) =>
      case Failure(_) => clearInterval(pokingHandle)
    }
  }

  private val receivedCredentials: Var[List[GameUserCredentials]] = Var(List())

  /** Fetch game information each time the game has changed in some way. */
  val $gameInfo: EventStream[MenuGameWithPlayers] =
    EventStream
      .merge(
        socket.$in.collect { case WebSocketProtocol.GameStatusUpdated => () }
          .debounce(500)
          .flatMap(_ => EventStream.fromZIOEffect(fetchingGameInfo)),
        EventStream.fromZIOEffect(fetchingGameInfo)
      )
      .collect { case Some(info) => info }

  /** Indicates whether the creator of the game is the current user. If yes, we display the cancel game button. */
  val $amICreator: EventStream[Boolean] = $gameInfo.map(_.game.gameCreator.userId == me.userId)

  /** Each time the game is cancelled (at most once), the game cancelled message is issued. */
  val $gameCancelled: EventStream[Unit] = socket.$in.collect { case WebSocketProtocol.GameCancelled => () }

  val $shouldMoveBackToHome: EventStream[Unit] = EventStream
    .merge(
      $gameCancelled,
      socket.$closed,
      socket.$error.mapTo(())
    )
    .flatMap(_ => EventStream.fromZIOEffect(moveTo(RouteDefinitions.homeRoute).provideLayer(layer)))

  val $updateCreds: EventStream[Unit] = socket.$in.collect {
    case WebSocketProtocol.GameUserCredentialsWrapper(gameUserCredentials) =>
      gameUserCredentials
  }.map { creds =>
    receivedCredentials.update(_ :+ creds)
  }

  /**
    * Connecting to the game server requires a one-time credentials that the server is going to provide.
    * The following observable emits each time the credentials come back from the web socket.
    */
  val $tokenForWebSocket: EventStream[String] = socket.$in.collect {
    case WebSocketProtocol.GameUserCredentialsWrapper(gameUserCredentials) =>
      gameUserCredentials
  }.flatMap(creds => EventStream.fromZIOEffect(fetchGameToken(creds).provideLayer(layer)))

  val $moveToGame: EventStream[Unit] = $tokenForWebSocket.flatMap(
    token => EventStream.fromZIOEffect(moveTo(inGame, gameIdParam & tokenParam)((gameId, token)).provideLayer(layer))
  )

  val cancelGameBus = new EventBus[Unit]
  val $cancelGame: EventStream[Int] =
    cancelGameBus.events.flatMap(_ => EventStream.fromZIOEffect(sendCancelGame(gameId).provideLayer(layer)))

  val startGameBus = new EventBus[Unit]
  val $startGame: EventStream[Int] =
    startGameBus.events.flatMap(
      _ =>
        EventStream.fromZIOEffect(
          sendLaunchGame(gameId).provideLayer(layer)
        )
    )

  val leaveGameBus = new EventBus[Unit]
  val $leaveGame: EventStream[Int] =
    leaveGameBus.events.flatMap(_ => EventStream.fromZIOEffect(iAmLeaving(gameId).provideLayer(layer)))

  val elem: ReactiveHtmlElement[html.Element] = section(
    mainContentContainer,
    className <-- $shouldMoveBackToHome.mapTo(""), // kicking off stream
    className <-- $cancelGame.mapTo(""), // kicking off stream
    className <-- $leaveGame.mapTo(""), // kicking off stream
    className <-- $updateCreds.mapTo(""), // kicking off stream // todo: delete this
    className <-- $startGame.mapTo(""),
    className <-- $moveToGame.mapTo(""), // kicking off stream
    div(
      mainContent,
      h1(
        className := "text-3xl",
        className := s"text-$primaryColour-$primaryColourDark",
        child.text <-- $gameInfo.map(_.game.gameName).map("Game " + _)
      ),
      child <-- EventStream
        .fromZIOEffect(fetchingGameInfo)
        .map(_.flatMap(_.game.gameConfiguration.playersInfo.get(me.userName)))
        .collect { case Some(value) => value }
        .map(
          info =>
            PlayerInfoOptionPanel(
              info,
              socket.outWriter.contramap[PlayerInfo](
                WebSocketProtocol.UpdateMyInfo(me.userId, _)
              )
            )
        ),
      PlayerList($gameInfo.map(_.game.gameConfiguration.playersInfo.values.toList)),
      div(
        child <-- $amICreator.map {
          if (_)
            button(
              btn,
              primaryButton,
              "Launch game!",
              onClick.mapTo(()) --> startGameBus,
              disabled <-- $gameInfo.map(!_.game.everyBodyReady)
            )
          else emptyNode
        },
        child <-- $amICreator.map {
          if (_)
            button(btn, secondaryButton, "Cancel game", onClick.mapTo(()) --> cancelGameBus)
          else
            button(
              btn,
              secondaryButton,
              "Leave game",
              onClick.mapTo(WebSocketProtocol.PlayerLeavesGame(me.userId)) --> socket.outWriter
            )
        }
      ),
      pre(
        child.text <-- receivedCredentials.signal.map(_.asJson.spaces2),
        child.text <-- $tokenForWebSocket
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
