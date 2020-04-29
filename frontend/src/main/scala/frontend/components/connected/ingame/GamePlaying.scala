package frontend.components.connected.ingame

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.LifecycleComponent
import gamelogic.gamestate.{ActionCollector, GameState}
import io.circe.syntax._
import models.bff.Routes._
import models.bff.ingame.InGameWSProtocol
import models.bff.ingame.InGameWSProtocol.{AddAndRemoveActions, HeartBeat, Ping, Pong, Ready}
import models.users.User
import org.scalajs.dom.html
import utils.websocket.JsonWebSocket
import zio.{Task, UIO, ZIO}
import utils.laminarzio.Implicits._

final class GamePlaying private (gameId: String, user: User, token: String) extends LifecycleComponent[html.Div] {

  private val layer = zio.clock.Clock.live

  val actionCollector: ActionCollector = new ActionCollector(GameState.initialGameState(0))

  final val gameSocket = JsonWebSocket[InGameWSProtocol, InGameWSProtocol, (String, String)](
    joinGameServer,
    userIdAndTokenParams,
    (user.userId, token),
    host = "localhost:22222" // todo: change this!
  )

  val $gameState: EventStream[GameState] = gameSocket.$in.collect { case msg: AddAndRemoveActions => msg }
    .map {
      case AddAndRemoveActions(actionsToAdd, oldestTimeToRemove, idsOfActionsToRemove) =>
        actionsToAdd.foreach(actionCollector.addAction(_, needUpdate = false))
        actionCollector.removeActions(oldestTimeToRemove, idsOfActionsToRemove)
        actionCollector.currentGameState
    }

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
    GameViewContainer(actionCollector.currentGameState, $gameState),
    pre(
      child.text <-- gameSocket.$in.filterNot(_ == HeartBeat)
        .filterNot(_.isInstanceOf[InGameWSProtocol.AddAndRemoveActions])
        .fold(List[InGameWSProtocol]())(_ :+ _)
        .map(_.map(_.asJson.spaces2))
        .map(_.mkString("\n"))
    )
  )

  override def componentDidMount(): Unit = {
    println(s"Game id: $gameId.")
    gameSocket
      .open()(elem)

    gameSocket.$open.flatMap(
      _ =>
        EventStream.fromZIOEffect(
          programs.frontend.ingame
            .synchronizeClock(sendPing(_)(elem))
            .zipLeft(ZIO.effectTotal(gameSocket.outWriter.onNext(Ready(user.userId))))
            .provideLayer(layer)
        )
    ).foreach(delta => println(s"Delta is: $delta"))(elem)

  }

}

object GamePlaying {
  def apply(gameId: String, user: User, token: String) = new GamePlaying(gameId, user, token)
}
