package gamelogic.abilities.boss.boss105

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.buffs.boss.boss105.Yearning
import gamelogic.docs.{AbilityInfoFromMetadata, AbilityMetadata}
import gamelogic.entities.boss.boss105.Shade
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.boss105.PutYearning
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** The boss calls its Shade: for a few seconds, the Shade rushes towards the boss, ignoring
  * everything else. The twins resonate as soon as they meet, and it takes the tank of the Shade to
  * bring it back where it belongs. A stunned Shade does not move.
  */
final case class Reunion(useId: Ability.UseId, time: Long, casterId: Entity.Id)
    extends Ability
    with AbilityInfoFromMetadata[Reunion.type] {
  def metadata = Reunion

  def cost: Resource.ResourceAmount = Resource.noResourceAmount

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    gameState
      .allTEntities[Shade]
      .keys
      .map(shadeId => PutYearning(genActionId(), time, genBuffId(), shadeId, casterId))
      .toVector

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Reunion =
    copy(time = newTime, useId = newId)

  /** There must be a Shade to call, and it must not already be on its way. */
  def canBeCast(gameState: GameState, time: Long): Option[String] =
    Option.unless(
      gameState
        .allTEntities[Shade]
        .keys
        .exists(shadeId => !gameState.hasBuffOfType[Yearning](shadeId))
    )("There is no Shade to call")
}

object Reunion extends AbilityMetadata {

  val cooldown: Long    = 36000L
  val castingTime: Long = 2000L

  val timeToFirstAbility: Long = 24000L

  def name: String = "Reunion"

  def abilityId: AbilityId = Ability.boss105Reunion
}
