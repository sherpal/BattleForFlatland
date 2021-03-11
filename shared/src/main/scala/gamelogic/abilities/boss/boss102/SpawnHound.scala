package gamelogic.abilities.boss.boss102

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.docs.{AbilityInfoFromMetadata, AbilityMetadata}
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.boss102.AddBossHound
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.utils.IdGeneratorContainer

final case class SpawnHound(useId: Ability.UseId, time: Long, casterId: Entity.Id, position: Complex)
    extends Ability
    with AbilityInfoFromMetadata[SpawnHound.type] {
  def metadata = SpawnHound

  def cost: Resource.ResourceAmount = Resource.ResourceAmount(0.0, Resource.NoResource)

  def createActions(gameState: GameState)(implicit idGeneratorContainer: IdGeneratorContainer): List[GameAction] =
    List(
      AddBossHound(
        idGeneratorContainer.gameActionIdGenerator(),
        time,
        idGeneratorContainer.entityIdGenerator(),
        position
      )
    )

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability = copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): None.type = None
}

object SpawnHound extends AbilityMetadata {

  @inline final def castingTime: Long = 2000L
  @inline final def cooldown: Long    = 10000L

  @inline final def timeToFirstAbility: Long = 13000L

  def name: String = "Spawn Hound"

  def abilityId: Ability.AbilityId = Ability.boss102SpawnBossHound
}
