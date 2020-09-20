package gamelogic.abilities.boss.boss103

import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.{Ability, WithTargetAbility}
import gamelogic.docs.AbilityMetadata
import gamelogic.entities.Resource.{NoResource, ResourceAmount}
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.boss103.PutInflamedDebuff
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** Puts an [[gamelogic.buffs.boss.boss103.Inflamed]] on the target. */
final case class HolyFlame(useId: Ability.UseId, time: Long, casterId: Entity.Id, targetId: Entity.Id)
    extends WithTargetAbility {
  def range: Distance = Double.MaxValue

  def abilityId: AbilityId = Ability.boss103HolyFlameId

  def cooldown: Long = HolyFlame.cooldown

  def castingTime: Long = HolyFlame.castingTime

  def cost: Resource.ResourceAmount = ResourceAmount(0, NoResource)

  def createActions(gameState: GameState)(implicit idGeneratorContainer: IdGeneratorContainer): List[GameAction] =
    List(
      PutInflamedDebuff(
        id       = idGeneratorContainer.gameActionIdGenerator(),
        time     = time,
        buffId   = idGeneratorContainer.buffIdGenerator(),
        bearerId = targetId,
        sourceId = casterId
      )
    )

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): HolyFlame = copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): Boolean = true
}

object HolyFlame extends AbilityMetadata {

  val cooldown: Long           = 4000L
  val timeToFirstAbility: Long = 8000L

  def name: String = "Holy Flame"

  def castingTime: Long = 0L
}
