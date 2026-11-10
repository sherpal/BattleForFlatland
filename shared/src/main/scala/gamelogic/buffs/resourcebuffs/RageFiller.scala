package gamelogic.buffs.resourcebuffs

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, PassiveBuff, WithSourceId}
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
) extends PassiveBuff
    with WithSourceId {

  def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Entity.Id])(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector.empty

  def actionTransformer(gameAction: GameAction): Vector[GameAction] = gameAction match {
    case gameAction: EntityTakesDamage if gameAction.entityId == bearerId =>
      Vector(
        gameAction,
        EntityResourceChanges(
          gameAction.id,
          gameAction.time,
          bearerId,
          gameAction.amount * RageFiller.ragePerDamage,
          Rage
        )
      )
    case _ => Vector(gameAction)
  }

  /** The damage must be seen before it is reduced by other buffs. */
  override def transformerOrder: Int = -1

  def duration: Long = -1L

  def resourceIdentifier: ResourceIdentifier = Buff.rageFiller
}

object RageFiller {

  /** Rage gained by the bearer for each point of damage they take, before any reduction. */
  inline def ragePerDamage: Double = 1.0

}
