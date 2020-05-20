package gamelogic.abilities.boss.boss101

import gamelogic.abilities.Ability
import gamelogic.entities.Entity
import gamelogic.entities.Resource
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.{BuffIdGenerator, EntityIdGenerator}
import gamelogic.entities.Resource.ResourceAmount
import gamelogic.entities.Resource.NoResource
import gamelogic.gamestate.gameactions.EntityTakesDamage

final case class BigHit(useId: Ability.UseId, time: Long, casterId: Entity.Id, targetId: Entity.Id) extends Ability {

  def abilityId: Ability.AbilityId = Ability.boss101BigHitId

  def cooldown: Long = 120000

  def castingTime: Long = 1000

  def cost: Resource.ResourceAmount = ResourceAmount(0, NoResource)

  def createActions(
      gameState: GameState
  )(implicit entityIdGenerator: EntityIdGenerator, buffIdGenerator: BuffIdGenerator): List[GameAction] =
    List(
      EntityTakesDamage(
        0L,
        time,
        targetId,
        BigHit.damageAmount,
        casterId
      )
    )

  def copyWithNewTimeAndId(newTime: Long, newId: Ability.UseId): Ability = copy(time = newTime, useId = newId)

}

object BigHit {

  final def damageAmount: Double = 150.0

}
