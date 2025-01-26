package gamelogic.buffs.resourcebuffs

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, PassiveBuff}
import gamelogic.entities.Entity
import gamelogic.entities.Resource.Rage
import gamelogic.gamestate.gameactions.{EntityResourceChanges, EntityTakesDamage}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

final case class RageFiller(
    buffId: Buff.Id,
    bearerId: Entity.Id,
    sourceId: Entity.Id,
    appearanceTime: Long
) extends PassiveBuff {

  def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Entity.Id])(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector.empty

  def actionTransformer(gameAction: GameAction): Vector[GameAction] = gameAction match {
    case gameAction: EntityTakesDamage if gameAction.entityId == bearerId =>
      Vector(gameAction, EntityResourceChanges(gameAction.id, gameAction.time, bearerId, 5.0, Rage))
    case _ => Vector(gameAction)
  }

  def duration: Long = -1L

  def resourceIdentifier: ResourceIdentifier = Buff.rageFiller
}
