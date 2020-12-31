package gamelogic.buffs.abilities.classes

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, PassiveBuff}
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.EntityStartsCasting
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.gameactions.RemoveBuff
import gamelogic.buffs.ActionPreventerBuff
import gamelogic.gamestate.gameactions.MovingBodyMoves
import gamelogic.gamestate.gameactions.UseAbility

final case class TriangleStunDebuff(buffId: Buff.Id, bearerId: Entity.Id, appearanceTime: Long) 
  extends PassiveBuff with ActionPreventerBuff {
  def endingAction(gameState: GameState, time: Long)(
      implicit idGeneratorContainer: IdGeneratorContainer
  ): List[GameAction] = Nil

  def isActionPrevented(action: GameAction): Boolean = action match {
    case action: MovingBodyMoves if action.entityId == bearerId => true
    case action: EntityStartsCasting if action.ability.casterId == bearerId => true
    case action: UseAbility if action.casterId == bearerId => true
    case _ => false
  }

  def actionTransformer(gameAction: GameAction): List[GameAction] = gameAction match {
    case action :EntityTakesDamage if action.entityId == bearerId =>
      // Debuff is removed when entity takes damage (it still takes the damages, though)
      List(action, RemoveBuff(action.id, action.time, bearerId, buffId))
    case action if isActionPrevented(action) => Nil
    case action => List(action)
  }

  def duration: Long = TriangleStunDebuff.duration

  def resourceIdentifier: Buff.ResourceIdentifier = Buff.triangleStun

}

object TriangleStunDebuff {
  def duration: Long = 20000L
}