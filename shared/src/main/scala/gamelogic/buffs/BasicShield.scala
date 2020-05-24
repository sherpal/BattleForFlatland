package gamelogic.buffs

import gamelogic.buffs.Buff.{Id, ResourceIdentifier}
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

final case class BasicShield(buffId: Buff.Id, bearerId: Entity.Id, appearanceTime: Long) extends PassiveBuff {
  def initialActions(gameState: GameState)(implicit idGeneratorContainer: IdGeneratorContainer): List[GameAction] = Nil

  def endingAction(gameState: GameState)(implicit idGeneratorContainer: IdGeneratorContainer): List[GameAction] = Nil

  def actionTransformer(gameAction: GameAction): List[GameAction] =
    (gameAction match {
      case gameAction: EntityTakesDamage if gameAction.entityId == bearerId =>
        gameAction.copy(amount = gameAction.amount * 0.8)
      case _ => gameAction
    }) :: Nil

  def duration: Long = Long.MaxValue

  def resourceIdentifier: ResourceIdentifier = Buff.squareDefaultShield
}
