package gamelogic.buffs.boss.sirofvice

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, TickerBuff}
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** Deals an increasing amount of damage over time (`baseDamage` on the first tick, then twice that,
  * and so on). Can be dispelled.
  */
final case class Inflamed(
    buffId: Buff.Id,
    bearerId: Entity.Id,
    appearanceTime: Long,
    lastTickTime: Long,
    sourceId: Entity.Id
) extends TickerBuff {
  def tickEffect(gameState: GameState, time: Long)(using IdGeneratorContainer): Vector[GameAction] =
    Vector(
      EntityTakesDamage(
        id = genActionId(),
        time = time,
        entityId = bearerId,
        amount = Inflamed.baseDamage * math.round((time - appearanceTime).toDouble / tickRate),
        sourceId = sourceId
      )
    )

  val tickRate: Long = Inflamed.tickRate

  def changeLastTickTime(time: Long): Inflamed = copy(lastTickTime = time)

  def duration: Long = Inflamed.duration

  def resourceIdentifier: ResourceIdentifier = Buff.sirOfViceInflamed

  def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Entity.Id])(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector.empty

  override def canBeDispelled: Boolean = true
}

object Inflamed {

  val tickRate: Long = 2000L

  /** Four ticks of 5, 10, 15 and 20 damage: half the life of most players if nobody dispels it. */
  val duration: Long = 8000L

  val baseDamage: Double = 5.0

}
