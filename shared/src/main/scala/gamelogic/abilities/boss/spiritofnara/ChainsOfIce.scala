package gamelogic.abilities.boss.spiritofnara

import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.{Ability, WithTargetAbility}
import gamelogic.docs.{AbilityInfoFromMetadata, AbilityMetadata}
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.spiritofnara.PutChained
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer
import services.i18n.I18n

/** Roots the target where it stands (see [[gamelogic.buffs.boss.spiritofnara.Chained]]). */
final case class ChainsOfIce(
    useId: Ability.UseId,
    time: Long,
    casterId: Entity.Id,
    targetId: Entity.Id
) extends WithTargetAbility
    with AbilityInfoFromMetadata[ChainsOfIce.type] {
  def metadata = ChainsOfIce

  def range: Distance = Double.MaxValue

  def cost: Resource.ResourceAmount = Resource.ResourceAmount(ChainsOfIce.manaCost, Resource.Mana)

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    gameState.players
      .get(targetId)
      .map(target =>
        PutChained(genActionId(), time, genBuffId(), targetId, target.currentPosition(time))
      )
      .toVector

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): ChainsOfIce =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long)(using I18n): None.type = None
}

object ChainsOfIce extends AbilityMetadata {
  val name: String             = "Chains of Ice"
  val cooldown: Long           = 14000L
  val castingTime: Long        = 0L
  val timeToFirstAbility: Long = 18000L

  inline def manaCost: Double = 15.0

  def abilityId: AbilityId = Ability.spiritOfNaraChainsOfIce
}
