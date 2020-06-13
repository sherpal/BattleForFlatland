package gamelogic.buffs

import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/**
  * A [[gamelogic.buffs.TickerBuff]] is a buff that does stuff at the given `tickRate`.
  * The "does stuff" is encoded in the `tickEffect` method.
  *
  * This is typically used for damage/heal over time (DoT/HoT).
  *
  * example:
  *          A ticker buff can heal the bearer for 10 health every 3 seconds, for 9 seconds.
  */
trait TickerBuff extends Buff {

  /** Generates actions when the buff ticks */
  def tickEffect(gameState: GameState, time: Long, idGenerator: IdGeneratorContainer): List[GameAction]

  /** Time (in millis) between two game states. */
  val tickRate: Long

  /** Time at which the ticker ticked the last time. */
  val lastTickTime: Long

  def changeLastTickTime(time: Long): TickerBuff

}
