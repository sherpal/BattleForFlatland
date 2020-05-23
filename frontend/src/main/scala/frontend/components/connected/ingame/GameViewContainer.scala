package frontend.components.connected.ingame

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import game.{GameAssetLoader, GameStateManager, Keyboard, Mouse}
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import models.bff.ingame.InGameWSProtocol.ReadyToStart
import models.bff.ingame.{InGameWSProtocol, KeyboardControls}
import models.syntax.Pointed
import models.users.User
import org.scalajs.dom.html
import typings.pixiJs.mod.Application
import typings.pixiJs.{AnonAntialias => ApplicationOptions}
import zio.ZIO

/**
  * The GameViewContainer is responsible for creating th instance of the [[game.GameStateManager]].
  *
  * It received an initial [[gamelogic.gamestate.GameState]] and a streams of game states, and it has to draw everything.
  * The way it does that is its business, but it will probably be customizable in the future.
  *
  * It will most likely be drawing on a canvas using Pixi, and perhaps using svg for life bars and stuff.
  */
final class GameViewContainer private (
    me: User,
    playerId: Entity.Id,
    $actionsFromServer: EventStream[gamelogic.gamestate.AddAndRemoveActions],
    socketOutWriter: Observer[InGameWSProtocol.Outgoing],
    deltaTimeWithServer: Long
) extends Component[html.Div] {

  private def container = element.ref

  val element: ReactiveHtmlElement[html.Div] = div(
    className := "GameViewContainer",
    onMountCallback(ctx => componentDidMount(ctx.owner))
  )

  val application: Application = new Application(ApplicationOptions(backgroundColor = 0x1099bb))
  val loader                   = new GameAssetLoader(application)

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
        deltaTimeWithServer,
        resources
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
      mountEffect(container, owner)
    )(println(_))

}

object GameViewContainer {
  def apply(
      me: User,
      playerId: Entity.Id,
      $actionsFromServer: EventStream[gamelogic.gamestate.AddAndRemoveActions],
      socketOutWriter: Observer[InGameWSProtocol.Outgoing],
      deltaTimeWithServer: Long
  ): GameViewContainer =
    new GameViewContainer(me, playerId, $actionsFromServer, socketOutWriter, deltaTimeWithServer)
}
