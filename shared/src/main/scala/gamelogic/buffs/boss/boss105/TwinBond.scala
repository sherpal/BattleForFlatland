package gamelogic.buffs.boss.boss105

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, PassiveBuff}
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** Carried by Boss105 for as long as its Shade lives: the boss takes much less damage, because the
  * Shade takes the blows for it. It is removed when the Shade falls (see [[ShadeBond]]), and put
  * back when the Shade is back.
  *
  * @param sourceId
  *   the Shade
  */
final case class TwinBond(
    buffId: Buff.Id,
    bearerId: Entity.Id,
    sourceId: Entity.Id,
    appearanceTime: Long
) extends PassiveBuff {
  def actionTransformer(gameAction: GameAction): Vector[GameAction] = gameAction match {
    case action: EntityTakesDamage if action.entityId == bearerId =>
      Vector(action.copy(amount = action.amount * TwinBond.damageMultiplier))
    case action => Vector(action)
  }

  def duration: Long = -1L

  def resourceIdentifier: ResourceIdentifier = Buff.boss105TwinBond

  def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Entity.Id])(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector.empty
}

object TwinBond {

  /** Multiplier applied to all the damage taken by the boss while its Shade lives. */
  inline def damageMultiplier: Double = 0.2
}
