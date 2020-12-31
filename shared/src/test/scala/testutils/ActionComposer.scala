package testutils

import gamelogic.gamestate.GameAction
import gamelogic.gamestate.GameState

final class ActionComposer private (act: GameState => GameState) {

  def >>(next: GameAction): ActionComposer = new ActionComposer(
    gameState => next(this.act(gameState))
  )

  def >>>(next: GameState => List[GameAction]): ActionComposer = new ActionComposer( (gameState: GameState) => {
      val untilNow = this.act(gameState)
      val newActions = next(untilNow)
      untilNow.applyActions(newActions)
    })
  

  def tap(sideEffect: GameState => Unit): ActionComposer = ActionComposer {
    (gameState: GameState) =>
      val untilNow = this.act(gameState)
      sideEffect(untilNow)
      untilNow
  }

  def >>>>(sideEffect: GameState => Unit): ActionComposer = tap(sideEffect)

  def apply(gameState: GameState): GameState = act(gameState)

}

object ActionComposer {
  val empty: ActionComposer = ActionComposer(identity)
  def fromAction(action: GameAction): ActionComposer = empty >> action

  private def apply(act: GameState => GameState): ActionComposer = new ActionComposer(act)
}