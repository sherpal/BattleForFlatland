package gamelogic.buffs.resourcebuffs

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, TickerBuff}
import gamelogic.entities.Entity
import gamelogic.entities.Resource.Energy
import gamelogic.gamestate.gameactions.EntityResourceChanges
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.EntityIdGenerator

/**
  * Never ending buff which grants its bearer 10 energy every second.
  *
  * This is a buff that the [[gamelogic.entities.classes.Triangle]] has by default.
  */
final case class EnergyFiller(buffId: Buff.Id, bearerId: Entity.Id, appearanceTime: Long, lastTickTime: Long)
    extends TickerBuff {
  def tickEffect(gameState: GameState, time: Long, entityIdGenerator: EntityIdGenerator): List[GameAction] = List(
    EntityResourceChanges(0L, time, bearerId, EnergyFiller.energyRefillPerSecond, Energy)
  )

  val tickRate: Long = 1000L

  def changeLastTickTime(time: Long): TickerBuff = copy(lastTickTime = time)

  def duration: Long = -1L

  def resourceIdentifier: ResourceIdentifier = Buff.energyFiller
}

object EnergyFiller {

  @inline final def energyRefillPerSecond = 10.0

}
