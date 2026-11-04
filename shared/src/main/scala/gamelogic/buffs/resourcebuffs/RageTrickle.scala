package gamelogic.buffs.resourcebuffs

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, TickerBuff}
import gamelogic.entities.Entity
import gamelogic.entities.Resource.Rage
import gamelogic.gamestate.gameactions.EntityResourceChanges
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** Never ending buff which grants its bearer a little rage every second, so that it is never
  * stuck waiting for the boss to hit it.
  *
  * This is a buff that the [[gamelogic.entities.classes.Square]] has by default, besides the
  * [[RageFiller]].
  */
final case class RageTrickle(
    buffId: Buff.Id,
    bearerId: Entity.Id,
    appearanceTime: Long,
    lastTickTime: Long
) extends TickerBuff {
  def tickEffect(
      gameState: GameState,
      time: Long
  )(using IdGeneratorContainer): Vector[GameAction] = Vector(
    EntityResourceChanges(
      genActionId(),
      time,
      bearerId,
      RageTrickle.rageRefillPerSecond,
      Rage
    )
  )

  val tickRate: Long = 1000L

  def changeLastTickTime(time: Long): TickerBuff = copy(lastTickTime = time)

  def duration: Long = -1L

  def resourceIdentifier: ResourceIdentifier = Buff.rageTrickle

  def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Entity.Id])(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector.empty
}

object RageTrickle {

  inline def rageRefillPerSecond: Double = 6.0

}
