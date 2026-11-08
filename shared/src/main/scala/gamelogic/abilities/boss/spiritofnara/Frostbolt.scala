package gamelogic.abilities.boss.spiritofnara

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.docs.{AbilityInfoFromMetadata, AbilityMetadata}
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.spiritofnara.PutChilled
import gamelogic.gamestate.gameactions.{EntityTakesDamage, RefreshMovementSpeed}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** The frost spell of the Spirit of Nara: damages its target, and slows it down (see
  * [[gamelogic.buffs.boss.spiritofnara.Chilled]]).
  */
final case class Frostbolt(
    useId: Ability.UseId,
    time: Long,
    casterId: Entity.Id,
    targetId: Entity.Id
) extends NaraNuke
    with AbilityInfoFromMetadata[Frostbolt.type] {
  def metadata = Frostbolt

  def school: Int = MagicSchool.frost

  def cost: Resource.ResourceAmount = Resource.ResourceAmount(Frostbolt.manaCost, Resource.Mana)

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    Vector(
      EntityTakesDamage(genActionId(), time, targetId, Frostbolt.damage, casterId),
      PutChilled(genActionId(), time, genBuffId(), targetId),
      RefreshMovementSpeed(genActionId(), time, targetId)
    )

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Frostbolt =
    copy(time = newTime, useId = newId)
}

object Frostbolt extends AbilityMetadata {
  val name: String             = "Frostbolt"
  val cooldown: Long           = 5000L
  val castingTime: Long        = 2000L
  val timeToFirstAbility: Long = 2000L

  inline def damage: Double   = 19.8
  inline def manaCost: Double = 40.0

  def abilityId: AbilityId = Ability.spiritOfNaraFrostbolt
}
