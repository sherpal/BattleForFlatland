package gamelogic.buffs

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

final case class BasicShield(buffId: Buff.Id, bearerId: Entity.Id, sourceId: Entity.Id, appearanceTime: Long)
    extends PassiveBuff {

  def endingAction(gameState: GameState, time: Long)(
      implicit idGeneratorContainer: IdGeneratorContainer
  ): List[GameAction] = Nil

  def actionTransformer(gameAction: GameAction): List[GameAction] =
    (gameAction match {
      case gameAction: EntityTakesDamage if gameAction.entityId == bearerId =>
        gameAction.copy(amount = gameAction.amount * 0.8)
      case _ => gameAction
    }) :: Nil

  def duration: Long = -1L

  def resourceIdentifier: ResourceIdentifier = Buff.squareDefaultShield
}
