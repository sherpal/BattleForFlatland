package gamelogic.buffs.boss.spiritofnara

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, TickerBuff}
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.EntityResourceChanges
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** The Spirit of Nara sits and drinks, regaining [[Drinking.manaPerTick]] mana at each tick. It
  * does nothing else meanwhile.
  */
final case class Drinking(
    buffId: Buff.Id,
    bearerId: Entity.Id,
    appearanceTime: Long,
    lastTickTime: Long
) extends TickerBuff {

  def tickEffect(gameState: GameState, time: Long)(using IdGeneratorContainer): Vector[GameAction] =
    Vector(
      EntityResourceChanges(genActionId(), time, bearerId, Drinking.manaPerTick, Resource.Mana)
    )

  val tickRate: Long = Drinking.tickRate

  def changeLastTickTime(time: Long): Drinking = copy(lastTickTime = time)

  def duration: Long = Drinking.duration

  def resourceIdentifier: ResourceIdentifier = Buff.spiritOfNaraDrinking

  def maybeSourceId: Option[Entity.Id] = None

  def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Entity.Id])(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector.empty
}

object Drinking {
  inline def duration: Long = 10000L
  inline def tickRate: Long = 1000L

  /** Ten ticks are enough to go from the threshold back to full mana. */
  inline def manaPerTick: Double = 150.0
}
