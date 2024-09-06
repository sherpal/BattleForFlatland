package gamelogic.abilities.square

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.buffs.Buff
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.PutConstantDot
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

final case class Enrage(useId: Ability.UseId, time: Long, casterId: Entity.Id) extends Ability {
  def abilityId: AbilityId = Ability.squareEnrageId

  def cooldown: Long = Enrage.cooldown

  def castingTime: Long = 0L

  def cost: Resource.ResourceAmount = Resource.ResourceAmount(0.0, Resource.Rage)

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    Vector(
      PutConstantDot(
        genActionId(),
        time,
        casterId,
        casterId,
        Enrage.damageOnTick,
        Enrage.duration,
        Enrage.tickRate,
        genBuffId(),
        Buff.squareEnrage
      )
    )

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): None.type = None
}

object Enrage {

  @inline final def cooldown     = 10000L
  @inline final def duration     = 10000L
  @inline final def damageOnTick = 5.0
  @inline final def tickRate     = 1000L

}
