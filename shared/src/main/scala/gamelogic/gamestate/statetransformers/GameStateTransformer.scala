package gamelogic.gamestate.statetransformers

import cats.kernel.Monoid
import gamelogic.gamestate.GameState

/**
  * A [[GameStateTransformer]] is a concrete materialization of a [[gamelogic.gamestate.GameAction]].
  *
  * Typically, a [[gamelogic.gamestate.GameAction]] generates one or more [[GameStateTransformer]] that concretely act
  * on the [[gamelogic.gamestate.GameState]]. This action transformers don't need to be serialized through communication
  * and can hence just be simple classes, involving other non-serializable objects.
  *
  * Important: [[GameStateTransformer]]s are not supposed to check whether their action make sense, but are merely
  * building blocks of the current [[gamelogic.gamestate.GameState]].
  */
trait GameStateTransformer extends (GameState => GameState) {
  final def ++(that: GameStateTransformer): GameStateTransformer =
    (gameState: GameState) => (this andThen that)(gameState)
}

object GameStateTransformer {
  def identityTransformer: GameStateTransformer = identity[GameState]

  implicit val monoid: Monoid[GameStateTransformer] = new Monoid[GameStateTransformer] {
    def empty: GameStateTransformer = identityTransformer

    def combine(x: GameStateTransformer, y: GameStateTransformer): GameStateTransformer = x ++ y
  }

}
