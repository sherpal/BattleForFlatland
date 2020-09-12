package frontend.components.connected.ingame

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import communication.BFFPicklers._
import frontend.components.Component
import frontend.components.utils.tailwind._
import gamelogic.entities.Entity
import gamelogic.gamestate
import gamelogic.physics.Complex
import models.bff.Routes._
import models.bff.ingame.InGameWSProtocol
import models.bff.ingame.InGameWSProtocol.{Ping, Pong, Ready, YourEntityIdIs}
import models.users.User
import org.scalajs.dom
import org.scalajs.dom.html
import services.http.FHttpClient
import utils.laminarzio.Implicits._
import utils.websocket.BoopickleWebSocket
import zio.{UIO, ZIO}

final class GamePlaying private (gameId: String, user: User, token: String) extends Component[html.Div] {

  private val layer = zio.clock.Clock.live ++ FHttpClient.live

  final val gameSocket = BoopickleWebSocket[InGameWSProtocol, InGameWSProtocol, (String, String)](
    joinGameServer,
    userIdAndTokenParams,
    (user.userId, token),
    host = dom.document.location.hostname + ":22222" // todo: change this!
  )

  val $actionsFromServer: EventStream[gamestate.AddAndRemoveActions] = gameSocket.$in.collect {
    case InGameWSProtocol.AddAndRemoveActions(actionsToAdd, oldestTimeToRemove, idsOfActionsToRemove) =>
      gamelogic.gamestate.AddAndRemoveActions(actionsToAdd, oldestTimeToRemove, idsOfActionsToRemove)
  }

  val $playerId: EventStream[Entity.Id] =
    EventStream
      .combine(
        EventStream.fromValue((), emitOnce = true), // take 1
        gameSocket.$in.collect { case YourEntityIdIs(id) => id }
      )
      .map(_._2)
  val $bossStartingPosition: EventStream[Complex] =
    EventStream
      .combine(
        EventStream.fromValue((), emitOnce = true),
        gameSocket.$in.collect { case InGameWSProtocol.StartingBossPosition(re, im) => Complex(re, im) }
      )
      .map(_._2)

  val deltaWithServerBus: EventBus[Long] = new EventBus

  def sendPing(ping: Ping)(implicit owner: Owner): UIO[Pong] =
    for {
      pongFiber <- ZIO
        .effectAsync[Any, Nothing, Pong](
          callback => gameSocket.$in.collect { case pong: Pong => pong }.map(UIO(_)).foreach(callback)
        )
        .fork
      _    <- ZIO.effectTotal(gameSocket.outWriter.onNext(ping))
      pong <- pongFiber.join
    } yield pong

  val element: ReactiveHtmlElement[html.Div] = div(
    className := "GamePlaying",
    child <-- $playerId.combineWith($bossStartingPosition).combineWith(deltaWithServerBus.events).map {
      case ((id, bossPosition), delta) =>
        GameViewContainer(user, id, bossPosition, $actionsFromServer, gameSocket.outWriter, delta)
    },
    child <-- gameSocket.$closed.map(_ => false)
      .startWith(true)
      .map(
        if (_)
          span(
            btn,
            secondaryButton,
            "Stop game",
            onClick --> { _ =>
              zio.Runtime.default
                .unsafeRunToFuture(programs.frontend.ingame.cancelGame(user, gameId, token).provideLayer(layer))
            }
          )
        else emptyNode
      ),
    onMountCallback(ctx => componentDidMount(ctx.owner)),
    child <-- gameSocket.$closed.map(
      _ =>
        span(
          btn,
          secondaryButton,
          onClick --> { _ =>
            dom.document.location.href = dom.document.location.origin.asInstanceOf[String]
          },
          "Back to home"
        )
    )
  )

  def componentDidMount(owner: Owner): Unit = {
    gameSocket
      .open()(owner)

//    gameSocket.$closed.foreach(_ => dom.document.location.href = dom.document.location.origin.asInstanceOf[String])(
//      owner
//    )
    gameSocket.$closed.foreach(_ => dom.console.warn("Socket connection closed."))(owner)

    gameSocket.$open.flatMap(
      _ =>
        programs.frontend.ingame
          .synchronizeClock(sendPing(_)(owner))
          .zipLeft(ZIO.effectTotal(gameSocket.outWriter.onNext(Ready(user.userId))))
          .provideLayer(layer)
    ).foreach(delta => {
      deltaWithServerBus.writer.onNext(delta.toLong)
    })(owner)

  }

}

object GamePlaying {
  def apply(gameId: String, user: User, token: String) = new GamePlaying(gameId, user, token)
}
