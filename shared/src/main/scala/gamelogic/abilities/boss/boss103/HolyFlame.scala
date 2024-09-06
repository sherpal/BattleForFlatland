package gamelogic.abilities.boss.boss103

import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.{Ability, WithTargetAbility}
import gamelogic.docs.{AbilityInfoFromMetadata, AbilityMetadata}
import gamelogic.entities.Resource.{NoResource, ResourceAmount}
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.boss103.PutInflamedDebuff
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** Puts an [[gamelogic.buffs.boss.boss103.Inflamed]] on the target. */
final case class HolyFlame(
    useId: Ability.UseId,
    time: Long,
    casterId: Entity.Id,
    targetId: Entity.Id
) extends WithTargetAbility
    with AbilityInfoFromMetadata[HolyFlame.type] {
  def range: Distance = Double.MaxValue

  def metadata = HolyFlame

  def cost: Resource.ResourceAmount = ResourceAmount(0, NoResource)

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    Vector(
      PutInflamedDebuff(
        id = genActionId(),
        time = time,
        buffId = genBuffId(),
        bearerId = targetId,
        sourceId = casterId
      )
    )

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): HolyFlame =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): None.type = None
}

object HolyFlame extends AbilityMetadata {

  val cooldown: Long           = 4000L
  val timeToFirstAbility: Long = 8000L

  def name: String = "Holy Flame"

  def castingTime: Long = 0L

  def abilityId: AbilityId = Ability.boss103HolyFlameId
}
