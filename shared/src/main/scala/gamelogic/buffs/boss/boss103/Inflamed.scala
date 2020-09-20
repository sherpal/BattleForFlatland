package gamelogic.buffs.boss.boss103

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, TickerBuff}
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** Deals an increasing amount of damage over time. */
final case class Inflamed(
    buffId: Buff.Id,
    bearerId: Entity.Id,
    appearanceTime: Long,
    lastTickTime: Long,
    sourceId: Entity.Id
) extends TickerBuff {
  def tickEffect(gameState: GameState, time: Long, idGenerator: IdGeneratorContainer): List[GameAction] =
    List(
      EntityTakesDamage(
        id       = idGenerator.gameActionIdGenerator(),
        time     = time,
        entityId = bearerId,
        amount =
          Inflamed.baseDamage * math.round((time - appearanceTime).toDouble / tickRate),
        sourceId = sourceId
      )
    )

  val tickRate: Long = Inflamed.tickRate

  def changeLastTickTime(time: Long): Inflamed = copy(lastTickTime = time)

  def duration: Long = Inflamed.duration

  def resourceIdentifier: ResourceIdentifier = Buff.boss103Inflamed

  def endingAction(gameState: GameState, time: Long)(
      implicit idGeneratorContainer: IdGeneratorContainer
  ): List[GameAction] = Nil

  override def canBeDispelled: Boolean = true
}

object Inflamed {

  val tickRate: Long = 2000L

  /**
    * Last tick will deal 80 damage which nearly kills the target.
    * This leaves a small chance to player to survive even if they mess up.
    */
  val duration: Long = 8000L

  val baseDamage: Double = 20.0

}
