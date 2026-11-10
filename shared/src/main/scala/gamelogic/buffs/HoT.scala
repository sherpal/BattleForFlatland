package gamelogic.buffs

import gamelogic.abilities.Ability
import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.EntityGetsHealed
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

trait HoT extends TickerBuff with WithSourceId {

  /** Entity responsible for putting the HoT on the target */
  val sourceId: Entity.Id

  /** Ability responsible for putting the HoT on the target */
  def abilityId: Ability.AbilityId

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
      sourceId,
      abilityId
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
      _resourceIdentifier: ResourceIdentifier,
      _abilityId: Ability.AbilityId
  ): HoT = new HoT {
    val buffId: Buff.Id = _buffId

    val sourceId: Entity.Id = _sourceId

    val abilityId: Ability.AbilityId = _abilityId

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
      _resourceIdentifier,
      _abilityId
    )

    def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Entity.Id])(using
        IdGeneratorContainer
    ): Vector[GameAction] = Vector.empty
  }

}
