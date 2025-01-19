package gamelogic.buffs

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

trait DoT extends TickerBuff {

  /** Entity responsible for putting the DoT on the target */
  val sourceId: Entity.Id

  /** Function specifying the damage that will be dealt after the `timeSinceBeginning`. We allow for
    * a function so that we can be a little bit more general than a constant damage. This can for
    * example allow to make a DoT which deals damage exponentially with respect to time (forcing
    * player to debuff early)
    */
  def damagePerTick(timeSinceBeginning: Long): Double

  final def tickEffect(
      gameState: GameState,
      time: Long
  )(using IdGeneratorContainer): Vector[GameAction] =
    Vector(
      EntityTakesDamage(
        genActionId(),
        time,
        bearerId,
        damagePerTick(
          time - appearanceTime
        ),
        sourceId
      )
    )

}

object DoT {

  def constantDot(
      currentTime: Long,
      targetId: Entity.Id,
      _buffId: Buff.Id,
      _duration: Long,
      _tickRate: Long,
      damageOnTick: Double,
      _sourceId: Entity.Id,
      _appearanceTime: Long,
      _resourceIdentifier: ResourceIdentifier
  ): DoT = new DoT {
    val buffId: Buff.Id = _buffId

    val sourceId: Entity.Id = _sourceId

    def damagePerTick(timeSinceBeginning: Long): Double = damageOnTick

    def resourceIdentifier: ResourceIdentifier = _resourceIdentifier

    val tickRate: Long       = _tickRate
    val bearerId: Entity.Id  = targetId
    val duration: Long       = _duration
    val appearanceTime: Long = _appearanceTime
    val lastTickTime: Long   = currentTime // should a ticker tick when it pops? I don't think so
    def changeLastTickTime(time: Long): TickerBuff = constantDot(
      time,
      targetId,
      _buffId,
      _duration,
      _tickRate,
      damageOnTick,
      _sourceId,
      _appearanceTime,
      _resourceIdentifier
    )

    def endingAction(gameState: GameState, time: Long)(using
        IdGeneratorContainer
    ): Vector[GameAction] = Vector.empty
  }

}
