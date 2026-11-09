package gamelogic.abilities.boss.spiritofnara

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.docs.{AbilityInfoFromMetadata, AbilityMetadata}
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** The fire spell of the Spirit of Nara: big damage on its target. */
final case class Fireball(
    useId: Ability.UseId,
    time: Long,
    casterId: Entity.Id,
    targetId: Entity.Id
) extends NaraNuke
    with AbilityInfoFromMetadata[Fireball.type] {
  def metadata = Fireball

  def school: Int = MagicSchool.fire

  def cost: Resource.ResourceAmount = Resource.ResourceAmount(Fireball.manaCost, Resource.Mana)

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    Vector(EntityTakesDamage(genActionId(), time, targetId, Fireball.damage, casterId, abilityId))

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Fireball =
    copy(time = newTime, useId = newId)
}

object Fireball extends AbilityMetadata {
  val name: String             = "Fireball"
  val cooldown: Long           = 6000L
  val castingTime: Long        = 2500L
  val timeToFirstAbility: Long = 3000L

  inline def damage: Double   = 33.0
  inline def manaCost: Double = 50.0

  def abilityId: AbilityId = Ability.spiritOfNara.fireballId
}
