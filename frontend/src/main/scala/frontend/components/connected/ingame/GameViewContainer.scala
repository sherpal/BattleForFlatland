package frontend.components.connected.ingame

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import game.ui.reactivepixi.{ReactivePixiElement, ReactiveStage}
import game.{GameAssetLoader, GameStateManager, Keyboard, Mouse, UserControls}
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import models.bff.ingame.InGameWSProtocol.ReadyToStart
import models.bff.ingame.{InGameWSProtocol, KeyboardControls}
import models.syntax.Pointed
import models.users.User
import org.scalajs.dom
import org.scalajs.dom.html
import org.scalajs.dom.html.Progress
import typings.pixiJs.anon.{Antialias => ApplicationOptions}
import typings.pixiJs.mod.Application
import zio.duration._
import zio.{Exit, UIO, ZIO}
import programs.frontend.menus.controls.retrieveControls

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

  private val gameSceneSizeRatio = 1200 / 800.0

  // We need to create the application with the correct size as it is the size used for the Camera at the beginning.
  val application: Application = new Application(
    ApplicationOptions(
      backgroundColor = 0x1099bb,
      width           = 1200,
      height          = 800
    )
  )
  val loader = new GameAssetLoader(application)

  val assetLoading: Signal[Double] =
    EventStream
      .merge(
        loader.$progressData.map(_.completion),
        loader.endedLoadingEvent.mapTo(100.0)
      )
      .startWith(0.0)

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

  def addWindowResizeEventListener(stage: ReactiveStage): ZIO[Any, Nothing, Unit] =
    (for {
      window      <- UIO(dom.window)
      resizeQueue <- zio.Queue.unbounded[Unit]
      _ <- ZIO.effectTotal {
        window.addEventListener(
          "resize",
          (_: dom.Event) => zio.Runtime.default.unsafeRunToFuture(resizeQueue.offer(()))
        )
      }
      canvas <- UIO(stage.application.view)
      _ <- ZIO.effectTotal(zio.Runtime.default.unsafeRunToFuture((for {
        _ <- ZIO.sleep(500.millis)
        _ <- resizeQueue.take
        _ <- resizeQueue.takeAll // empty queue so that there is no buffering
        _ <- ZIO.effectTotal {
          val (canvasWidth, canvasHeight) = stage.computeApplicationViewDimension(
            window.innerWidth * 0.9,
            window.innerHeight * 0.9,
            gameSceneSizeRatio
          )
          canvas.width  = canvasWidth.toInt
          canvas.height = canvasHeight.toInt
          stage.resize()
        }

      } yield ()).forever.provideLayer(zio.clock.Clock.live)))
      _ <- resizeQueue.offer(()) // fixing the size at the beginning
    } yield ())

  private def mountEffect(gameContainer: html.Div, owner: Owner) =
    for {
      resources <- loader.loadAssets
      // todo!: remove hardcoded stuff
      stage    <- UIO(ReactivePixiElement.stage(application))
      _        <- addWindowResizeEventListener(stage)
      controls <- retrieveControls
      userControls = new UserControls(
        new Keyboard(controls),
        new Mouse(application.view.asInstanceOf[html.Canvas], controls)
      )
      _ = new GameStateManager(
        stage,
        GameState.empty,
        $actionsFromServer,
        socketOutWriter,
        userControls,
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
    utils.runtime.unsafeRunAsync(
      mountEffect(containerViewChild, owner).unit
    )((_: Exit[Nothing, Unit]) => ())

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
