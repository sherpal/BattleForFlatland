package gamelogic.buffs.resourcebuffs

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, TickerBuff}
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.EntityResourceChanges
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/**
  * Mana is restored by 10 every 10 seconds.
  */
final case class ManaFiller(buffId: Buff.Id, bearerId: Entity.Id, appearanceTime: Long, lastTickTime: Long)
    extends TickerBuff {
  def tickEffect(gameState: GameState, time: Long, entityIdGenerator: IdGeneratorContainer): List[GameAction] = List(
    EntityResourceChanges(
      0L,
      time,
      bearerId,
      ManaFiller.manaRefillPerSecond * ManaFiller.tickEveryNSeconds,
      Resource.Mana
    )
  )

  val tickRate: Long = 1000L * ManaFiller.tickEveryNSeconds

  def changeLastTickTime(time: Long): ManaFiller = copy(lastTickTime = time)

  def duration: Long = -1L

  def resourceIdentifier: ResourceIdentifier = Buff.manaFiller

  def initialActions(gameState: GameState, time: Long)(
      implicit idGeneratorContainer: IdGeneratorContainer
  ): List[GameAction] = Nil

  def endingAction(gameState: GameState, time: Long)(
      implicit idGeneratorContainer: IdGeneratorContainer
  ): List[GameAction] = Nil
}

object ManaFiller {

  @inline final def manaRefillPerSecond: Double = 1.0
  @inline final def tickEveryNSeconds: Int      = 10
}
