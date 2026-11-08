package gamelogic.abilities.boss.spiritofnara

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.docs.{AbilityInfoFromMetadata, AbilityMetadata}
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer
import services.i18n.I18n

/** Cast right after drinking: big damage to every player. */
final case class Pyroblast(useId: Ability.UseId, time: Long, casterId: Entity.Id)
    extends Ability
    with AbilityInfoFromMetadata[Pyroblast.type] {
  def metadata = Pyroblast

  def cost: Resource.ResourceAmount = Resource.ResourceAmount(0, Resource.Mana)

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    gameState.players.keys.toVector
      .sortBy(_.value)
      .map(playerId =>
        EntityTakesDamage(genActionId(), time, playerId, Pyroblast.damage, casterId, abilityId)
      )

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Pyroblast =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long)(using I18n): None.type = None
}

object Pyroblast extends AbilityMetadata {
  val name: String             = "Pyroblast"
  val cooldown: Long           = 1000L
  val castingTime: Long        = 3000L
  val timeToFirstAbility: Long = 0L

  inline def damage: Double = 70.0

  def abilityId: AbilityId = Ability.spiritOfNaraPyroblast
}
