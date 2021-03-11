package gamelogic.buffs.ai

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, PassiveBuff}
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.{EntityTakesDamage, ThreatToEntityChange}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/**
  * Each time the bearer takes damage, it adds a amount of threat towards the source of the damage proportional to
  * the amount of damage.
  */
final case class DamageThreatAware(buffId: Buff.Id, bearerId: Entity.Id, sourceId: Entity.Id, appearanceTime: Long)
    extends PassiveBuff {

  def endingAction(gameState: GameState, time: Long)(
      implicit idGeneratorContainer: IdGeneratorContainer
  ): List[GameAction] = Nil

  def actionTransformer(gameAction: GameAction): List[GameAction] = gameAction match {
    case gameAction: EntityTakesDamage if gameAction.entityId == bearerId =>
      List(
        gameAction,
        ThreatToEntityChange(
          gameAction.id,
          gameAction.time,
          bearerId,
          gameAction.sourceId,
          gameAction.amount,
          isDamageThreat = true
        )
      )
    case _ => List(gameAction)
  }

  def duration: Long = -1L

  def resourceIdentifier: ResourceIdentifier = Buff.damageThreatAware
}
