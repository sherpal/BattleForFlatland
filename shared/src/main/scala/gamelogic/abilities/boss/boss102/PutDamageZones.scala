package gamelogic.abilities.boss.boss102

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.docs.{AbilityInfoFromMetadata, AbilityMetadata}
import gamelogic.entities.boss.boss102.DamageZone
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.boss102.PutDamageZone
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

final case class PutDamageZones(
    useId: Ability.UseId,
    time: Long,
    casterId: Entity.Id,
    targetIds: Vector[Entity.Id]
) extends Ability
    with AbilityInfoFromMetadata[PutDamageZones.type] {
  def metadata = PutDamageZones

  def cost: Resource.ResourceAmount = Resource.ResourceAmount(0, Resource.NoResource)

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    targetIds
      .flatMap(gameState.players.get)
      .map(_.pos)
      .map(
        PutDamageZone(
          genActionId(),
          time,
          genEntityId(),
          _,
          DamageZone.radius,
          casterId,
          Entity.teams.mobTeam,
          genBuffId()
        )
      )

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): PutDamageZones =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): None.type = None
}

object PutDamageZones extends AbilityMetadata {

  def name = "Damage Zone"

  inline def cooldown: Long    = 15000L
  inline def castingTime: Long = 3000L

  inline def timeToFirstAbility: Long = 3000L

  def abilityId: Ability.AbilityId = Ability.boss102PutDamageZones

}
