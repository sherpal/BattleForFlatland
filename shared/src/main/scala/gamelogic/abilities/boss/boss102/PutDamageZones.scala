package gamelogic.abilities.boss.boss102

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.docs.{AbilityInfoFromMetadata, AbilityMetadata}
import gamelogic.entities.boss.boss102.DamageZone
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.boss102.PutDamageZone
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

final case class PutDamageZones(useId: Ability.UseId, time: Long, casterId: Entity.Id, targetIds: List[Entity.Id])
    extends Ability
    with AbilityInfoFromMetadata[PutDamageZones.type] {
  def metadata = PutDamageZones

  def cost: Resource.ResourceAmount = Resource.ResourceAmount(0, Resource.NoResource)

  def createActions(gameState: GameState)(implicit idGeneratorContainer: IdGeneratorContainer): List[GameAction] =
    targetIds
      .flatMap(gameState.players.get)
      .map(_.pos)
      .map(
        PutDamageZone(
          idGeneratorContainer.gameActionIdGenerator(),
          time,
          idGeneratorContainer.entityIdGenerator(),
          _,
          DamageZone.radius,
          casterId,
          Entity.teams.mobTeam,
          idGeneratorContainer.buffIdGenerator()
        )
      )

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): PutDamageZones = copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): None.type = None
}

object PutDamageZones extends AbilityMetadata {

  def name = "Damage Zone"

  @inline final def cooldown: Long    = 30000L
  @inline final def castingTime: Long = 3000L

  @inline final def timeToFirstAbility: Long = 3000L

  def abilityId: Ability.AbilityId = Ability.boss102PutDamageZones

}
