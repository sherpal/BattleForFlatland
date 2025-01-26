package gamelogic.buffs

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

final case class BasicShield(
    buffId: Buff.Id,
    bearerId: Entity.Id,
    sourceId: Entity.Id,
    appearanceTime: Long
) extends PassiveBuff {

  def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Entity.Id])(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector.empty

  def actionTransformer(gameAction: GameAction): Vector[GameAction] =
    Vector(gameAction match {
      case gameAction: EntityTakesDamage if gameAction.entityId == bearerId =>
        gameAction.copy(amount = gameAction.amount * 0.8)
      case _ => gameAction
    })

  def duration: Long = -1L

  def resourceIdentifier: ResourceIdentifier = Buff.squareDefaultShield
}
