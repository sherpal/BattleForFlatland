package components.duringgame

import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import communication.BFFPicklers.*
import gamelogic.entities.Entity
import gamelogic.gamestate
import gamelogic.physics.Complex
import models.bff.Routes.*
import models.bff.ingame.InGameWSProtocol
import models.bff.ingame.InGameWSProtocol.{Ping, Pong, Ready, YourEntityIdIs}
import org.scalajs.dom
import org.scalajs.dom.html
import utils.laminarzio.*
import utils.websocket.BoopickleWebSocket
import zio.*
import menus.data.User
import models.bff.ingame.GameUserCredentials
import be.doeraene.webcomponents.ui5.*
import services.FrontendEnv
import be.doeraene.webcomponents.ui5.configkeys.ButtonDesign
import models.bff.ingame.ClockSynchronizationReport

object GamePlaying {
  def apply(gameId: String, user: User, secret: String, port: Int)(using
      Runtime[FrontendEnv]
  ): HtmlElement = {

    def gameCredentials = GameUserCredentials(user.name, gameId, secret)

    val gameSocket =
      BoopickleWebSocket[InGameWSProtocol, InGameWSProtocol, GameUserCredentials](
        joinGameServer,
        gameUserCredentialsParam,
        gameCredentials,
        host = dom.document.location.hostname ++ s":$port"
      )

    val actionsFromServerEvents: EventStream[gamestate.AddAndRemoveActions] =
      gameSocket.inEvents.collect {
        case InGameWSProtocol.AddAndRemoveActions(
              actionsToAdd,
              oldestTimeToRemove,
              idsOfActionsToRemove
            ) =>
          gamelogic.gamestate.AddAndRemoveActions(
            actionsToAdd,
            oldestTimeToRemove,
            idsOfActionsToRemove
          )
      }

    val playerIdEvents: EventStream[Entity.Id] =
      gameSocket.inEvents.collect { case YourEntityIdIs(id) => id }.take(1)

    val bossStartingPositionEvents: EventStream[Complex] =
      gameSocket.inEvents
        .collect { case InGameWSProtocol.StartingBossPosition(re, im) =>
          Complex(re, im)
        }
        .take(1)

    val clockSyncReportBus                   = new EventBus[ClockSynchronizationReport]
    val deltaWithServerEvents                = clockSyncReportBus.events.map(_.deltaAsLong)
    val clockSyncReportProgressPercentageBus = new EventBus[Int]
    val clockSyncReportProgressPercentageSignal =
      clockSyncReportProgressPercentageBus.events.startWith(0)

    def sendPing(ping: Ping)(using Owner): UIO[Pong] =
      for {
        pongFiber <- ZIO
          .asyncZIO[Any, Nothing, Pong](callback =>
            ZIO.succeed(
              gameSocket.inEvents
                .collect { case pong: Pong => pong }
                .map(ZIO.succeed(_))
                .foreach(callback)
            )
          )
          .fork
        _    <- Clock.sleep(10.millis)
        _    <- ZIO.succeed(gameSocket.outWriter.onNext(ping))
        pong <- pongFiber.join
      } yield pong

    def gameProtocolModifier: Modifier[HtmlElement] =
      Vector[Modifier[HtmlElement]](
        onMountBind(ctx =>
          gameSocket.openEvents
            .flatMapSwitchZIO(_ =>
              programs.frontend.ingame
                .synchronizeClock(
                  sendPing(_)(using ctx.owner),
                  postedOnProgress =
                    perc => ZIO.succeed(clockSyncReportProgressPercentageBus.writer.onNext(perc))
                )
                .zipLeft(ZIO.succeed(gameSocket.outWriter.onNext(Ready(user.name))))
            ) --> clockSyncReportBus.writer
        ),
        clockSyncReportBus.events.map(_.printableString) --> Observer[String](println(_)),
        gameSocket.modifier
      )

    def clockSyncProgressBar = div(
      Label("Synchronizing clocks..."),
      br(),
      ProgressIndicator(
        _.value <-- clockSyncReportProgressPercentageSignal,
        width    := "200px"
      )
    )

    div(
      className := "GamePlaying",
      child.maybe <-- clockSyncReportProgressPercentageSignal
        .map(_ < 100)
        .distinct
        .map(Option.when(_)(clockSyncProgressBar)),
      child <-- playerIdEvents
        .combineWith(bossStartingPositionEvents, deltaWithServerEvents)
        .map { (id, bossPosition, delta) =>
          ???
        // GameViewContainer(user, id, bossPosition, $actionsFromServer, gameSocket.outWriter, delta)
        },
      child <-- gameSocket.closedSignal
        .map(!_)
        .map(
          if _ then
            Button(
              "Stop game",
              _.events.onClick.mapToUnit --> Observer.fromZIO[Any] { _ =>
                programs.frontend.ingame.cancelGame(gameCredentials).orDie
              }
            )
          else emptyNode
        ),
      gameProtocolModifier,
      gameSocket.closedSignal.changes.filter(identity) --> Observer[Any](_ =>
        dom.console.warn("Socket connection closed")
      ),
      child.maybe <-- gameSocket.closedSignal.map(
        Option.when(_)(
          Button(
            _.events.onClick --> { _ =>
              dom.document.location.href = dom.document.location.origin
            },
            "Back to home",
            _.design := ButtonDesign.Emphasized
          )
        )
      ),
      gameSocket.errorEvents --> Observer[Any](x => dom.console.warn(x))
    )
  }
}
