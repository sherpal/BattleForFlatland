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

final case class TriangleStunDebuff(
    buffId: Buff.Id,
    bearerId: Entity.Id,
    sourceId: Entity.Id,
    appearanceTime: Long
) extends PassiveBuff
    with ActionPreventerBuff {
  def endingAction(gameState: GameState, time: Long)(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector.empty

  def isActionPrevented(action: GameAction): Boolean = action match {
    case action: MovingBodyMoves if action.entityId == bearerId             => true
    case action: EntityStartsCasting if action.ability.casterId == bearerId => true
    case action: UseAbility if action.casterId == bearerId                  => true
    case _                                                                  => false
  }

  def actionTransformer(gameAction: GameAction): Vector[GameAction] = gameAction match {
    case action: EntityTakesDamage if action.entityId == bearerId =>
      // Debuff is removed when entity takes damage (it still takes the damages, though)
      Vector(action, RemoveBuff(action.id, action.time, bearerId, buffId))
    case action if isActionPrevented(action) => Vector.empty
    case action                              => Vector(action)
  }

  def duration: Long = TriangleStunDebuff.duration

  def resourceIdentifier: Buff.ResourceIdentifier = Buff.triangleStun

}

object TriangleStunDebuff {
  def duration: Long = 20000L
}
