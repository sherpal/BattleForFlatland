package gamelogic.abilities.boss.boss110.bigguyabilities

import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.{Ability, WithTargetAbility}
import gamelogic.docs.AbilityMetadata
import gamelogic.entities.boss.dawnoftime.Boss110
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer
import gamelogic.buffs.boss.boss110.BrokenArmor
import gamelogic.gamestate.gameactions.RemoveBuff
import gamelogic.gamestate.gameactions.PutSimpleBuff
import gamelogic.buffs.Buff

/**
  * Puts The [[gamelogic.buffs.boss.boss110.BrokenArmor]] debuff on the target.
  */
final case class PutBrokenArmor(useId: Ability.UseId, time: Long, casterId: Entity.Id, targetId: Entity.Id)
    extends WithTargetAbility {

  def abilityId: Ability.AbilityId = Ability.boss110BigGuyBrokenArmor

  def cooldown: Long = PutBrokenArmor.cooldown

  def castingTime: Long = PutBrokenArmor.castingTime

  def cost: Resource.ResourceAmount = Resource.ResourceAmount(0.0, Resource.NoResource)

  def createActions(gameState: GameState)(implicit idGeneratorContainer: IdGeneratorContainer): List[GameAction] =
    // First we remove the previous debuff, if any
    gameState
      .allBuffsOfEntity(targetId)
      .collect {
        case buff: BrokenArmor if buff.sourceId == casterId => buff
      }
      .map(buff => RemoveBuff(idGeneratorContainer.gameActionIdGenerator(), time, buff.bearerId, buff.buffId))
      .toList :+ PutSimpleBuff(
      idGeneratorContainer.gameActionIdGenerator(),
      time,
      idGeneratorContainer.buffIdGenerator(),
      targetId,
      casterId,
      time,
      Buff.boss110BrokenArmor
    )

  def copyWithNewTimeAndId(newTime: Long, newId: Ability.UseId): Ability =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): Option[String] =
    isInRangeAndInSight(gameState, time) orElse canBeCastEnemyOnly(gameState)

  def range: WithTargetAbility.Distance = WithTargetAbility.meleeRange
}

object PutBrokenArmor extends AbilityMetadata {

  def name: String = "Put Broken Armor"

  def cooldown: Long = 1000L

  def castingTime: Long = 0L

  def timeToFirstAbility: Long = 0L

}
