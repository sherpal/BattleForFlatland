package frontend.components.connected.ingame

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.LifecycleComponent
import gamelogic.entities.Entity
import gamelogic.gamestate
import gamelogic.gamestate.{ActionCollector, GameState}
import io.circe.syntax._
import models.bff.Routes._
import models.bff.ingame.InGameWSProtocol
import models.bff.ingame.InGameWSProtocol.{HeartBeat, Ping, Pong, Ready, YourEntityIdIs}
import models.users.User
import org.scalajs.dom
import org.scalajs.dom.html
import typings.pixiJs.mod.Application
import typings.pixiJs.{AnonAntialias => ApplicationOptions}
import utils.laminarzio.Implicits._
import utils.websocket.JsonWebSocket
import zio.{UIO, ZIO}

final class GamePlaying private (gameId: String, user: User, token: String) extends LifecycleComponent[html.Div] {

  private val layer = zio.clock.Clock.live
  val application   = new Application(ApplicationOptions(backgroundColor = 0x1099bb))

  val actionCollector: ActionCollector = new ActionCollector(GameState.initialGameState(0))

  final val gameSocket = JsonWebSocket[InGameWSProtocol, InGameWSProtocol, (String, String)](
    joinGameServer,
    userIdAndTokenParams,
    (user.userId, token),
    host = "localhost:22222" // todo: change this!
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

  val deltaWithServerBus: EventBus[Long] = new EventBus

  def sendPing(ping: Ping)(implicit owner: Owner): UIO[Pong] =
    for {
      pongFiber <- ZIO
        .effectAsync[Any, Nothing, Pong](
          callback => gameSocket.$in.collect { case pong: Pong => pong }.map(UIO(_)).foreach(callback)
        )
        .fork
      _ <- ZIO.effectTotal(gameSocket.outWriter.onNext(ping))
      pong <- pongFiber.join
    } yield pong

  val elem: ReactiveHtmlElement[html.Div] = div(
    className := "GamePlaying",
    child <-- $playerId.combineWith(deltaWithServerBus.events).map {
      case (id, delta) => GameViewContainer(user, id, $actionsFromServer, gameSocket.outWriter, delta)
    }
  )

  override def componentDidMount(): Unit = {
    println(s"Game id: $gameId.")
    gameSocket
      .open()(elem)

    gameSocket.$closed.foreach(_ => dom.document.location.href = dom.document.location.origin.asInstanceOf[String])(
      elem
    )

    gameSocket.$open.flatMap(
      _ =>
        EventStream.fromZIOEffect(
          programs.frontend.ingame
            .synchronizeClock(sendPing(_)(elem))
            .zipLeft(ZIO.effectTotal(gameSocket.outWriter.onNext(Ready(user.userId))))
            .provideLayer(layer)
        )
    ).foreach(delta => {
      println(s"Delta is: $delta")
      deltaWithServerBus.writer.onNext(delta.toLong)
    })(elem)

  }

}

object GamePlaying {
  def apply(gameId: String, user: User, token: String) = new GamePlaying(gameId, user, token)
}
