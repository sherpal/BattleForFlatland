package gamelogic.buffs

import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.EntityGetsHealed
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.EntityIdGenerator

trait HoT extends TickerBuff {

  /** Entity responsible for putting the HoT on the target */
  val sourceId: Entity.Id

  /**
    * Function specifying the heal that will be done after the `timeSinceBeginning`.
    */
  def healPerTick(timeSinceBeginning: Long): Double

  final def tickEffect(
      gameState: GameState,
      time: Long,
      entityIdGenerator: EntityIdGenerator
  ): List[GameAction] = List(
    EntityGetsHealed(0L, time, bearerId, healPerTick(time - appearanceTime), sourceId)
  )

}
