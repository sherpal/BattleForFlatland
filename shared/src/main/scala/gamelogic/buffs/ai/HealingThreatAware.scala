package gamelogic.buffs.ai

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, PassiveBuff}
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.{EntityGetsHealed, ThreatToEntityChange}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/**
  * An entity with this buff will increase the healing threats of enemies when their heal one of their
  * allies.
  */
final case class HealingThreatAware(buffId: Buff.Id, bearerId: Entity.Id, appearanceTime: Long) extends PassiveBuff {
  def initialActions(gameState: GameState, time: Long)(
      implicit idGeneratorContainer: IdGeneratorContainer
  ): List[GameAction] = Nil

  def endingAction(gameState: GameState, time: Long)(
      implicit idGeneratorContainer: IdGeneratorContainer
  ): List[GameAction] = Nil

  def actionTransformer(gameAction: GameAction): List[GameAction] = gameAction match {
    case gameAction: EntityGetsHealed =>
      List(
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
    case _ => List(gameAction)
  }

  def duration: Long = -1

  def resourceIdentifier: ResourceIdentifier = Buff.healingThreatAware
}
