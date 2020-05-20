package gamelogic.abilities.boss.boss101

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.entities.Entity
import gamelogic.entities.Resource
import gamelogic.entities.Resource.{NoResource, ResourceAmount}
import gamelogic.gamestate.gameactions.PutConstantDot
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.{BuffIdGenerator, EntityIdGenerator}

final case class BigDot(useId: Ability.UseId, time: Long, casterId: Entity.Id, targetId: Entity.Id) extends Ability {
  def abilityId: AbilityId = Ability.boss101BigDotId

  def cooldown: Long = 20000L

  def castingTime: Long = 0L

  def cost: Resource.ResourceAmount = ResourceAmount(0, NoResource)

  def createActions(
      gameState: GameState
  )(implicit entityIdGenerator: EntityIdGenerator, buffIdGenerator: BuffIdGenerator): List[GameAction] =
    List(
      PutConstantDot(
        0L,
        time,
        targetId,
        casterId,
        BigDot.damageOnTick,
        BigDot.duration,
        BigDot.tickRate,
        buffIdGenerator()
      )
    )

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability = copy(time = newTime, useId = newId)
}

object BigDot {

  final def damageOnTick: Double = 20.0
  final def duration: Long       = 60000L
  final def tickRate: Long       = 3000L

}
