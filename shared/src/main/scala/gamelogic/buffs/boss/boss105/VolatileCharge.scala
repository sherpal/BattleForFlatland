package gamelogic.buffs.boss.boss105

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, PassiveBuff}
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** A charge that explodes when it ends (or when its bearer dies), hurting every player around the
  * bearer, the bearer included. Dispelling it makes it harmless.
  *
  * @param sourceId
  *   the boss
  */
final case class VolatileCharge(
    buffId: Buff.Id,
    bearerId: Entity.Id,
    sourceId: Entity.Id,
    appearanceTime: Long
) extends PassiveBuff {
  def actionTransformer(gameAction: GameAction): Vector[GameAction] = Vector(gameAction)

  def duration: Long = VolatileCharge.duration

  def resourceIdentifier: ResourceIdentifier = Buff.boss105VolatileCharge

  override def canBeDispelled: Boolean = true

  def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Entity.Id])(using
      IdGeneratorContainer
  ): Vector[GameAction] =
    if maybeDispelledBy.isDefined then Vector.empty
    else
      gameState.players.get(bearerId).toVector.flatMap { bearer =>
        val centre = bearer.currentPosition(time)
        gameState.players.valuesIterator
          .filter(_.currentPosition(time).distanceTo(centre) < VolatileCharge.radius)
          .map(player =>
            EntityTakesDamage(genActionId(), time, player.id, VolatileCharge.damage, sourceId)
          )
          .toVector
      }
}

object VolatileCharge {

  val duration: Long = 8000L

  /** Players closer than this to the bearer when the charge explodes take damage. */
  inline def radius: Double = 110.0

  inline def damage: Double = 35.0
}
