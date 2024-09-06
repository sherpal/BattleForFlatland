package gamelogic.abilities.triangle

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.buffs.Buff
import gamelogic.entities.Resource.{Energy, ResourceAmount}
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.PutSimpleBuff
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** Puts the [[gamelogic.buffs.abilities.classes.UpgradeDirectHit]] on the caster.
  */
final case class UpgradeDirectHit(useId: Ability.UseId, time: Long, casterId: Entity.Id)
    extends Ability {
  def abilityId: AbilityId = Ability.triangleUpgradeDirectHit

  def cooldown: Long = 0L

  def castingTime: Long = 0L

  def cost: Resource.ResourceAmount = UpgradeDirectHit.cost

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    Vector(
      PutSimpleBuff(
        genActionId(),
        time,
        genBuffId(),
        casterId,
        casterId,
        time,
        Buff.triangleUpgradeDirectHit
      )
    )

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): None.type = None
}

object UpgradeDirectHit {

  final val cost: ResourceAmount = ResourceAmount(50.0, Energy)

}
