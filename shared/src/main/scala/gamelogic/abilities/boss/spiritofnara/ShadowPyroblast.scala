package gamelogic.abilities.boss.spiritofnara

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.docs.{AbilityInfoFromMetadata, AbilityMetadata}
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer
import services.i18n.I18n

/** The berserk of the Spirit of Nara: once the fight lasted too long, it hits every player with
  * deadly shadow fire, again and again.
  */
final case class ShadowPyroblast(useId: Ability.UseId, time: Long, casterId: Entity.Id)
    extends Ability
    with AbilityInfoFromMetadata[ShadowPyroblast.type] {
  def metadata = ShadowPyroblast

  def cost: Resource.ResourceAmount = Resource.ResourceAmount(0, Resource.Mana)

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    gameState.players.keys.toVector
      .sortBy(_.value)
      .map(playerId =>
        EntityTakesDamage(
          genActionId(),
          time,
          playerId,
          ShadowPyroblast.damage,
          casterId,
          abilityId
        )
      )

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): ShadowPyroblast =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long)(using I18n): None.type = None
}

object ShadowPyroblast extends AbilityMetadata {
  val name: String             = "Shadow Pyroblast"
  val cooldown: Long           = 3000L
  val castingTime: Long        = 1000L
  val timeToFirstAbility: Long = 8 * 60 * 1000L

  inline def damage: Double = 165.0

  def abilityId: AbilityId = Ability.spiritOfNara.shadowPyroblastId
}
