package frontend.components.connected.ingame

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.LifecycleComponent
import game.{GameStateManager, Keyboard}
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import models.bff.ingame.{InGameWSProtocol, KeyboardControls}
import models.syntax.Pointed
import org.scalajs.dom.html

/**
  * The GameViewContainer is responsible for creating th instance of the [[game.GameStateManager]].
  *
  * It received an initial [[gamelogic.gamestate.GameState]] and a streams of game states, and it has to draw everything.
  * The way it does that is its business, but it will probably be customizable in the future.
  *
  * It will most likely be drawing on a canvas using Pixi, and perhaps using svg for life bars and stuff.
  */
final class GameViewContainer private (
    playerId: Entity.Id,
    $actionsFromServer: EventStream[gamelogic.gamestate.AddAndRemoveActions],
    socketOutWriter: Observer[InGameWSProtocol.Outgoing]
) extends LifecycleComponent[html.Div] {

  private def container = elem.ref

  val elem: ReactiveHtmlElement[html.Div] = div(
    className := "GameViewContainer"
  )

  // todo: remove hardcoded stuff
  val gameStateManager: GameStateManager = new GameStateManager(
    GameState.initialGameState(0L),
    $actionsFromServer,
    socketOutWriter,
    new Keyboard(implicitly[Pointed[KeyboardControls]].unit),
    playerId
  )(elem)

  override def componentDidMount(): Unit =
    container.appendChild(gameStateManager.application.view.asInstanceOf[html.Canvas])

}

object GameViewContainer {
  def apply(
      playerId: Entity.Id,
      $actionsFromServer: EventStream[gamelogic.gamestate.AddAndRemoveActions],
      socketOutWriter: Observer[InGameWSProtocol.Outgoing]
  ): GameViewContainer =
    new GameViewContainer(playerId, $actionsFromServer, socketOutWriter)
}
