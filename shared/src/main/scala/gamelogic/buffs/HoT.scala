package gamelogic.buffs

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.EntityGetsHealed
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

trait HoT extends TickerBuff {

  /** Entity responsible for putting the HoT on the target */
  val sourceId: Entity.Id

  /** Function specifying the heal that will be done after the `timeSinceBeginning`.
    */
  def healPerTick(timeSinceBeginning: Long): Double

  final def tickEffect(
      gameState: GameState,
      time: Long
  )(using IdGeneratorContainer): Vector[GameAction] = Vector(
    EntityGetsHealed(
      genActionId(),
      time,
      bearerId,
      healPerTick(time - appearanceTime),
      sourceId
    )
  )

}

object HoT {

  def constantHot(
      currentTime: Long,
      targetId: Entity.Id,
      _buffId: Buff.Id,
      _duration: Long,
      _tickRate: Long,
      healOnTick: Double,
      _sourceId: Entity.Id,
      _appearanceTime: Long,
      _resourceIdentifier: ResourceIdentifier
  ): HoT = new HoT {
    val buffId: Buff.Id = _buffId

    val sourceId: Entity.Id = _sourceId

    def healPerTick(timeSinceBeginning: Long): Double = healOnTick

    def resourceIdentifier: ResourceIdentifier = _resourceIdentifier

    val tickRate: Long       = _tickRate
    val bearerId: Entity.Id  = targetId
    val duration: Long       = _duration
    val appearanceTime: Long = _appearanceTime
    val lastTickTime: Long   = currentTime // should a ticker tick when it pops? I don't think so
    def changeLastTickTime(time: Long): TickerBuff = constantHot(
      time,
      targetId,
      _buffId,
      _duration,
      _tickRate,
      healOnTick,
      _sourceId,
      _appearanceTime,
      _resourceIdentifier
    )

    def endingAction(gameState: GameState, time: Long)(using
        IdGeneratorContainer
    ): Vector[GameAction] = Vector.empty
  }

}
