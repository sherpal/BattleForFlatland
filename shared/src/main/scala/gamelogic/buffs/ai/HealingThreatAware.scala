package gamelogic.buffs.ai

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, PassiveBuff}
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.{EntityGetsHealed, ThreatToEntityChange}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** An entity with this buff will increase the healing threats of enemies when their heal one of
  * their allies.
  */
final case class HealingThreatAware(
    buffId: Buff.Id,
    bearerId: Entity.Id,
    sourceId: Entity.Id,
    appearanceTime: Long
) extends PassiveBuff {

  def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Entity.Id])(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector.empty

  def actionTransformer(gameAction: GameAction): Vector[GameAction] = gameAction match {
    case gameAction: EntityGetsHealed =>
      Vector(
        gameAction,
        ThreatToEntityChange(
          gameAction.id,
          gameAction.time,
          bearerId,
          gameAction.sourceId,
          gameAction.amount,
          isDamageThreat = false
        )
      )
    case _ => Vector(gameAction)
  }

  def duration: Long = -1

  def resourceIdentifier: ResourceIdentifier = Buff.healingThreatAware
}
