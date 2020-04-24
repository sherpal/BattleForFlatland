package gamelogic.gamestate

trait GameAction extends Ordered[GameAction] {

  val id: GameAction.Id

  /** Time at which the action occurred (in millis) */
  val time: Long

  /** Describes how this action affects a given GameState. */
  def apply(gameState: GameState): GameState

  final def compare(that: GameAction): Int = this.time compare that.time

}

object GameAction {

  type Id = Long

}
