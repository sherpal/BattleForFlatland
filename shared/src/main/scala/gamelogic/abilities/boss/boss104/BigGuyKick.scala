package gamelogic.abilities.boss.boss104

import gamelogic.abilities.{Ability, WithTargetAbility}
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.pentagon.PentaDispel
import gamelogic.abilities.triangle.Cut
import gamelogic.buffs.abilities.classes.Silenced
import gamelogic.entities.Resource.ResourceAmount
import gamelogic.entities.boss.boss104.BigGuy
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

case class BigGuyKick(useId: Ability.UseId, time: Long, casterId: Entity.Id, targetId: Entity.Id)
    extends WithTargetAbility {
  override def abilityId: AbilityId = Ability.boss104BigGuyKick

  override def cooldown: Long = BigGuyKick.cooldown

  override def castingTime: Long = BigGuyKick.castingTime

  override def cost: Resource.ResourceAmount = ResourceAmount(0, Resource.NoResource)

  override def canBeCut: Boolean = true

  override def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    Vector(EntityTakesDamage(genActionId(), time, targetId, BigGuyKick.damage, casterId))

  override def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability =
    copy(time = newTime, useId = newId)

  override def canBeCast(gameState: GameState, time: Long): Option[String] =
    isInRangeAndInSight(gameState, time).orElse {
      Option.when(gameState.hasBuffOfType[Silenced](casterId))("Entity is silenced")
    }

  override def range: Distance = BigGuyKick.range
}

object BigGuyKick {
  val castingTime: Long = 2000L

  val cooldown: Long = Cut.cooldown + 3000L

  val timeToFirstUse: Long = 3000L

  val damage: Double = 50.0

  val range: Distance = BigGuy.shape.radius * 2.2
}
