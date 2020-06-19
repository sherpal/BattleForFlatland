package frontend.components.connected.ingame

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import game.{GameAssetLoader, GameStateManager, Keyboard, Mouse}
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import models.bff.ingame.InGameWSProtocol.ReadyToStart
import models.bff.ingame.{InGameWSProtocol, KeyboardControls}
import models.syntax.Pointed
import models.users.User
import org.scalajs.dom.html
import org.scalajs.dom.html.Progress
import typings.pixiJs.anon.{Antialias => ApplicationOptions}
import typings.pixiJs.mod.Application
import zio.ZIO

/**
  * The GameViewContainer is responsible for creating the instance of the [[game.GameStateManager]].
  *
  * It received an initial [[gamelogic.gamestate.GameState]] and a streams of game states, and it has to draw everything.
  * The way it does that is its business, but it will probably be customizable in the future.
  *
  * It will most likely be drawing on a canvas using Pixi, and perhaps using svg for life bars and stuff.
  */
final class GameViewContainer private (
    me: User,
    playerId: Entity.Id,
    bossStartingPosition: Complex,
    $actionsFromServer: EventStream[gamelogic.gamestate.AddAndRemoveActions],
    socketOutWriter: Observer[InGameWSProtocol.Outgoing],
    deltaTimeWithServer: Long
) extends Component[html.Div] {

  private def containerViewChild: html.Div = element.ref.firstChild.asInstanceOf[html.Div]

  private val maybeTargetBus: EventBus[Option[Entity]] = new EventBus

  val application: Application = new Application(
    ApplicationOptions(
      backgroundColor = 0x1099bb,
      width           = 1200,
      height          = 800
    )
  )
  val loader = new GameAssetLoader(application)

  val assetLoading: Signal[Double] = loader.$progressData.map(_.completion).startWith(0.0)

  val loadingProgressBar: ReactiveHtmlElement[Progress] = progress(
    maxAttr := "100",
    value <-- assetLoading.map(_.toString)
  )

  val element: ReactiveHtmlElement[html.Div] = div(
    className := "GameViewContainer",
    className := "flex items-center justify-center",
    onMountCallback(ctx => componentDidMount(ctx.owner)),
    div(child <-- assetLoading.map(_ < 100).map(if (_) loadingProgressBar else emptyNode))
  )

  private def mountEffect(gameContainer: html.Div, owner: Owner) =
    for {
      resources <- loader.loadAssets
      // todo!: remove hardcoded stuff
      _ = new GameStateManager(
        application,
        GameState.empty,
        $actionsFromServer,
        socketOutWriter,
        new Keyboard(implicitly[Pointed[KeyboardControls]].unit),
        new Mouse(application.view.asInstanceOf[html.Canvas]),
        playerId,
        bossStartingPosition,
        deltaTimeWithServer,
        resources,
        maybeTargetBus.writer
      )(owner)
      _ <- ZIO.effectTotal(
        socketOutWriter.onNext(ReadyToStart(me.userId))
      )
      _ <- ZIO.effectTotal {
        gameContainer.appendChild(application.view.asInstanceOf[html.Canvas])
      }
    } yield ()

  def componentDidMount(owner: Owner): Unit =
    zio.Runtime.default.unsafeRunAsync(
      mountEffect(containerViewChild, owner)
    )(println(_))

}

object GameViewContainer {
  def apply(
      me: User,
      playerId: Entity.Id,
      bossStartingPosition: Complex,
      $actionsFromServer: EventStream[gamelogic.gamestate.AddAndRemoveActions],
      socketOutWriter: Observer[InGameWSProtocol.Outgoing],
      deltaTimeWithServer: Long
  ): GameViewContainer =
    new GameViewContainer(me, playerId, bossStartingPosition, $actionsFromServer, socketOutWriter, deltaTimeWithServer)
}
