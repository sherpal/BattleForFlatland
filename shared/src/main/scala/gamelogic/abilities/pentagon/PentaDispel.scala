package gamelogic.abilities.pentagon

import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.{Ability, WithTargetAbility}
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.RemoveBuff
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** Removes all dispel-able buffs from the friendly target.
  *
  * Note that it also remove all friendly buffs, so that you need to use this somehow carefully.
  */
final case class PentaDispel(
    useId: Ability.UseId,
    time: Long,
    casterId: Entity.Id,
    targetId: Entity.Id
) extends WithTargetAbility {
  def range: Distance = WithTargetAbility.healRange

  def abilityId: AbilityId = Ability.pentagonDispelId

  def cooldown: Long = PentaDispel.cooldown

  def castingTime: Long = 0L

  def cost: Resource.ResourceAmount = PentaDispel.cost

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    gameState
      .allBuffsOfEntity(targetId)
      .filter(_.canBeDispelled)
      .flatMap(buff =>
        RemoveBuff(
          id = genActionId(),
          time = time,
          bearerId = buff.bearerId,
          buffId = buff.buffId
        ) +: buff.endingAction(gameState, time, Some(casterId))
      )
      .toVector

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): PentaDispel =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): Option[String] =
    canBeCastFriendlyOnly(gameState) orElse isInRangeAndInSight(gameState, time)
}

object PentaDispel {

  val name: String = "Penta Dispel"

  val cooldown: Long                = 7000L
  val cost: Resource.ResourceAmount = Resource.ResourceAmount(5.0, Resource.Mana)

}
