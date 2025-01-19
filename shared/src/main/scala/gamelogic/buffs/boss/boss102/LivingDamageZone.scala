package gamelogic.buffs.boss.boss102

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, TickerBuff}
import gamelogic.entities.Entity
import gamelogic.entities.classes.Constants
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** The [[LivingDamageZone]] deals damage to all allies of the bearer (except themselves) within the
  * specified range.
  */
final case class LivingDamageZone(
    buffId: Buff.Id,
    bearerId: Entity.Id,
    appearanceTime: Long,
    lastTickTime: Long,
    damage: Double,
    sourceId: Entity.Id
) extends TickerBuff {
  def tickEffect(gameState: GameState, time: Long)(using IdGeneratorContainer): Vector[GameAction] =
    gameState.livingEntityAndMovingBodyById(bearerId).fold(Vector[GameAction]()) { bearer =>
      gameState.allLivingEntities
        .filter(_.teamId == bearer.teamId)
        .filterNot(_.id == bearer.id)
        .filter(ally => (ally.currentPosition(time) - bearer.pos).modulus < LivingDamageZone.range)
        .map(ally => EntityTakesDamage(genActionId(), time, ally.id, damage, sourceId))
        .toVector
    }

  val tickRate: Long = LivingDamageZone.tickRate

  def changeLastTickTime(time: Long): LivingDamageZone = copy(lastTickTime = time)

  def duration: Long = LivingDamageZone.duration

  def resourceIdentifier: ResourceIdentifier = Buff.boss102LivingDamageZone

  def endingAction(gameState: GameState, time: Long)(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector.empty
}

object LivingDamageZone {

  final val tickRate: Long = 1000L
  final val duration: Long = 60000L // cooldown will be half of this
  final val range: Double  = Constants.playerRadius * 4

}
