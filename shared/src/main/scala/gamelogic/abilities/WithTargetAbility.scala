package gamelogic.abilities

import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.entities.Entity
import gamelogic.entities.classes.Constants
import gamelogic.gamestate.GameState

trait WithTargetAbility extends Ability {

  def range: Distance
  def targetId: Entity.Id

  override def isInRange(gameState: GameState, time: Long): Boolean =
    (for {
      caster <- gameState.withPositionEntityById(casterId)
      target <- gameState.withPositionEntityById(targetId)
      casterPosition = caster.currentPosition(time)
      targetPosition = target.currentPosition(time)
      distance       = (casterPosition - targetPosition).modulus
    } yield distance <= range).getOrElse(false)

}

object WithTargetAbility {

  /** Represents a distance in the Complex plane (usually computed using (z1 - z2).modulus) */
  type Distance = Double

  def meleeRange: Distance = Constants.playerRadius * 2

  def healRange: Distance = 600.0
}
