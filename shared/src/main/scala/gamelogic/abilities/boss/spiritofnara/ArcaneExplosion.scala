package gamelogic.abilities.boss.spiritofnara

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.docs.{AbilityInfoFromMetadata, AbilityMetadata}
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer
import services.i18n.I18n

/** Deals deadly damage to every player within [[ArcaneExplosion.radius]] of the Spirit of Nara.
  *
  * It is always cast right after a [[MagneticPull]], so players have to run away from the boss
  * (slowed down) during the cast.
  */
final case class ArcaneExplosion(useId: Ability.UseId, time: Long, casterId: Entity.Id)
    extends Ability
    with AbilityInfoFromMetadata[ArcaneExplosion.type] {
  def metadata = ArcaneExplosion

  def cost: Resource.ResourceAmount = Resource.ResourceAmount(0, Resource.Mana)

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    gameState
      .withPositionEntityById(casterId)
      .fold(Vector.empty[GameAction]) { caster =>
        val centre = caster.currentPosition(time)
        gameState.players.values.toVector
          .sortBy(_.id.value)
          .filter(_.currentPosition(time).distanceTo(centre) < ArcaneExplosion.radius)
          .map(player =>
            EntityTakesDamage(
              genActionId(),
              time,
              player.id,
              ArcaneExplosion.damage,
              casterId,
              abilityId
            )
          )
      }

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): ArcaneExplosion =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long)(using I18n): None.type = None
}

object ArcaneExplosion extends AbilityMetadata {
  val name: String             = "Arcane Explosion"
  val cooldown: Long           = 1000L
  val castingTime: Long        = 6000L
  val timeToFirstAbility: Long = 0L

  inline def radius: Double = 230.0
  inline def damage: Double = 275.0

  def abilityId: AbilityId = Ability.spiritOfNara.arcaneExplosionId
}
