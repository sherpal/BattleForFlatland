package frontend.components.connected.menugames.gamejoined

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import errors.ErrorADT
import frontend.components.Component
import frontend.components.utils.loading.LoadingScreen
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
import services.routing._
import utils.laminarzio.Implicits.{zioFlattenStrategy, _}
import utils.websocket.JsonWebSocket
import zio.ZIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.scalajs.js.timers.{clearInterval, setInterval, SetIntervalHandle}
import scala.util.{Failure, Success}

final class GameJoined private (gameId: String, me: User) extends Component[html.Element] {

  private val socket = JsonWebSocket[WebSocketProtocol, WebSocketProtocol, String](gameJoinedWS, gameIdParam, gameId)

  private val fetchingGameInfo = fetchGameInfo(gameId)

  /** Contains the interval handle to poke the presence to the server. */
  private val pokingHandleVar: Var[Option[SetIntervalHandle]] = Var(None)

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
    .flatMap(_ => moveTo(RouteDefinitions.homeRoute))

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
  }.flatMap(creds => EventStream.fromZIOEffect(fetchGameToken(creds)))

  val $moveToGame: EventStream[Unit] = $tokenForWebSocket.flatMap { token =>
    moveTo(inGame, gameIdParam & tokenParam)((gameId, token))
  }

  val cancelGameBus = new EventBus[Unit]
  val $cancelGame: EventStream[Int] =
    cancelGameBus.events.flatMap(_ => EventStream.fromZIOEffect(sendCancelGame(gameId)))

  val startGameBus = new EventBus[Unit]
  val $startGame: EventStream[Either[ErrorADT, Int]] =
    startGameBus.events.flatMap(
      _ =>
        sendLaunchGame(gameId)
          .tap(_ => ZIO.effectTotal(socket.outWriter.onNext(WebSocketProtocol.GameLaunched)))
          .either
    )

  val leaveGameBus = new EventBus[Unit]
  val $leaveGame: EventStream[Int] =
    leaveGameBus.events.flatMap(_ => EventStream.fromZIOEffect(iAmLeaving(gameId)))

  val element: ReactiveHtmlElement[html.Element] = section(
    mainContentContainer,
    className <-- $shouldMoveBackToHome.mapTo(""), // kicking off stream
    className <-- $cancelGame.mapTo(""), // kicking off stream
    className <-- $leaveGame.mapTo(""), // kicking off stream
    className <-- $moveToGame.mapTo(""), // kicking off stream
    div(
      mainContent,
      h1(
        className := "text-3xl",
        className := s"text-$primaryColour-$primaryColourDark",
        child.text <-- $gameInfo.map(_.game.gameName).map("Game " + _)
      ),
      child <-- socket.$open.flatMap(_ => fetchingGameInfo)
        .collect { case Some(info) => info }
        .map(
          info =>
            div(
              child <-- EventStream
                .fromValue(info, emitOnce = false)
                .map(_.game.gameConfiguration.playersInfo.get(me.userName))
                .collect { case Some(playerInfo) => playerInfo }
                .map(
                  playerInfo =>
                    PlayerInfoOptionPanel(
                      playerInfo,
                      socket.outWriter.contramap[PlayerInfo](
                        WebSocketProtocol.UpdateMyInfo(me.userId, _)
                      )
                    )
                ),
              child <-- EventStream
                .fromValue(info, emitOnce = false)
                .filter(_.game.gameCreator.userName == me.userName)
                .mapTo(GameOptionPanel(info, socket.outWriter)),
              PlayerList($gameInfo.map(_.game.gameConfiguration.playersInfo.values.toList)),
              div(
                child <-- $amICreator.map {
                  if (_)
                    button(
                      btn,
                      className <-- $gameInfo.map(_.game.gameConfigurationIsValid)
                        .startWith(false)
                        .map(if (_) primaryButtonContent else primaryButtonDisabledContent),
                      "Launch game!",
                      onClick.mapTo(()) --> startGameBus,
                      disabled <-- $gameInfo.map(!_.game.gameConfigurationIsValid)
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
              BossDescription($gameInfo.startWith(info).map(_.game.gameConfiguration.maybeBossName))
            )
        ),
      position := "relative",
      child <-- socket.$in.filter(_ == WebSocketProtocol.GameLaunched).mapTo(LoadingScreen()),
      child <-- $startGame.collect { // todo: improve error management
        case Left(error) =>
          pre(
            "Something went terribly wrong when launching the game:\n",
            error.asJson.spaces2
          )
      }
    ),
    onMountCallback(ctx => {
      socket.open()(ctx.owner)
      pokingHandleVar.set(
        Some(
          setInterval(10.seconds) {
            utils.runtime.unsafeRunToFuture(pokingPresence(gameId)) onComplete {
              case Success(_) =>
              case Failure(_) => pokingHandleVar.now.foreach(clearInterval)
            }
          }
        )
      )
    }),
    onUnmountCallback { _ =>
      socket.close()
      pokingHandleVar.now.foreach(clearInterval)
    }
  )

}

object GameJoined {
  def apply(gameId: String, me: User) = new GameJoined(gameId, me)
}
