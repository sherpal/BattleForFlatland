package gamelogic.abilities.boss.spiritofnara

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.docs.{AbilityInfoFromMetadata, AbilityMetadata}
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.spiritofnara.AddBlizzard
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer
import services.i18n.I18n

import scala.util.Random

/** Summons a [[gamelogic.entities.boss.spiritofnara.BlizzardCloud]] somewhere along the walls. It
  * goes around the room, hurting and slowing the players it passes over.
  */
final case class Blizzard(useId: Ability.UseId, time: Long, casterId: Entity.Id)
    extends Ability
    with AbilityInfoFromMetadata[Blizzard.type] {
  def metadata = Blizzard

  def cost: Resource.ResourceAmount = Resource.ResourceAmount(Blizzard.manaCost, Resource.Mana)

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    Vector(
      AddBlizzard(
        genActionId(),
        time,
        genEntityId(),
        Angle.Tau * Angle.fromDouble(Random.nextDouble()),
        genBuffId(),
        casterId
      )
    )

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Blizzard =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long)(using I18n): None.type = None
}

object Blizzard extends AbilityMetadata {
  val name: String             = "Blizzard"
  val cooldown: Long           = MagneticPull.cooldown
  val castingTime: Long        = 2000L
  val timeToFirstAbility: Long = MagneticPull.timeToFirstAbility

  inline def manaCost: Double = 40.0

  def abilityId: AbilityId = Ability.spiritOfNara.blizzardId
}
