package gamelogic.buffs
import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.entities.Entity
import gamelogic.entities.Resource.Rage
import gamelogic.gamestate.gameactions.{EntityResourceChanges, EntityTakesDamage}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

final case class RageFiller(buffId: Buff.Id, bearerId: Entity.Id, appearanceTime: Long) extends PassiveBuff {
  def initialActions(gameState: GameState)(implicit idGeneratorContainer: IdGeneratorContainer): List[GameAction] = Nil

  def endingAction(gameState: GameState)(implicit idGeneratorContainer: IdGeneratorContainer): List[GameAction] = Nil

  def actionTransformer(gameAction: GameAction): List[GameAction] = gameAction match {
    case gameAction: EntityTakesDamage if gameAction.entityId == bearerId =>
      List(gameAction, EntityResourceChanges(gameAction.id, gameAction.time, bearerId, 5.0, Rage))
    case _ => List(gameAction)
  }

  def duration: Long = -1L

  def resourceIdentifier: ResourceIdentifier = Buff.rageFiller
}
