package gamelogic.entities.classes.pentagon

import gamelogic.entities.Entity
import gamelogic.entities.classes.Pentagon
import gamelogic.gamestate.GameState

/** The longer a [[Pentagon]] stays still, the more damage it deals: it gains a stack every
  * `SteadyAim.stackEvery`, up to `SteadyAim.maxStacks`, and loses them all as soon as it moves.
  *
  * The stacks are not a buff: they come from the time since which the Pentagon is still (its
  * `stillSince`). The damage of its abilities is increased when they are used (when the bullets are
  * fired, not when they hit).
  */
object SteadyAim {

  /** Time (in millis) the Pentagon has to stay still to gain a stack. */
  inline def stackEvery: Long = 2000L

  inline def maxStacks: Int = 5

  /** Damage increase for each stack. */
  inline def damageIncreasePerStack: Double = 0.02

  /** The stacks of `pentagon` at `time`. */
  def stacks(pentagon: Pentagon, time: Long): Int =
    if pentagon.moving then 0
    else ((time - pentagon.stillSince) / stackEvery).toInt.max(0).min(maxStacks)

  /** What the damage of the abilities of `casterId` at `time` is multiplied by (1 if it is not a
    * [[Pentagon]]).
    */
  def damageMultiplier(gameState: GameState, casterId: Entity.Id, time: Long): Double =
    gameState.players.get(casterId).fold(1.0) {
      case pentagon: Pentagon => 1 + damageIncreasePerStack * stacks(pentagon, time)
      case _ => 1.0
    }

}
