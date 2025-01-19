package gamelogic.buffs.boss.boss110

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, PassiveBuff}
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions._
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** Multiplies by 2 the amount of damage that the bearer takes.
  *
  * Note that if the bearer has n instance of this debuff, the damage are multiplied by 2^n, which
  * is on purpose!
  *
  * @param buffId
  *   buff id for this instance
  * @param bearerId
  *   entity id of the bearer
  * @param appearanceTime
  *   time at which the entity arrived
  */
final case class BrokenArmor(
    buffId: Buff.Id,
    bearerId: Entity.Id,
    sourceId: Entity.Id,
    appearanceTime: Long
) extends PassiveBuff {

  def duration: Long = BrokenArmor.duration

  def resourceIdentifier: Buff.ResourceIdentifier = Buff.boss110BrokenArmor

  def endingAction(gameState: GameState, time: Long)(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector.empty

  def actionTransformer(gameAction: GameAction): Vector[GameAction] = gameAction match {
    case takingDamage: EntityTakesDamage if takingDamage.entityId == bearerId =>
      Vector(takingDamage.copy(amount = takingDamage.amount * 2))
    case action => Vector(action)
  }
}

object BrokenArmor {
  def duration: Long = 5000L
}
