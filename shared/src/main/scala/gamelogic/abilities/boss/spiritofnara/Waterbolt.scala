package gamelogic.abilities.boss.spiritofnara

import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.{Ability, WithTargetAbility}
import gamelogic.docs.{AbilityInfoFromMetadata, AbilityMetadata}
import gamelogic.entities.Resource.{NoResource, ResourceAmount}
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer
import services.i18n.I18n

/** Cast by the Water Elementals at random players. Can be cut. */
final case class Waterbolt(
    useId: Ability.UseId,
    time: Long,
    casterId: Entity.Id,
    targetId: Entity.Id
) extends WithTargetAbility
    with AbilityInfoFromMetadata[Waterbolt.type] {
  def metadata = Waterbolt

  def range: Distance = Double.MaxValue

  override def canBeCut: Boolean = true

  def cost: Resource.ResourceAmount = ResourceAmount(0, NoResource)

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    Vector(EntityTakesDamage(genActionId(), time, targetId, Waterbolt.damage, casterId, abilityId))

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Waterbolt =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long)(using I18n): None.type = None
}

object Waterbolt extends AbilityMetadata {
  val name: String             = "Waterbolt"
  val cooldown: Long           = 1500L
  val castingTime: Long        = 2000L
  val timeToFirstAbility: Long = 2000L

  inline def damage: Double = 11.0

  def abilityId: AbilityId = Ability.spiritOfNara.waterboltId
}
