package gamelogic.abilities.boss.spiritofnara

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.docs.{AbilityInfoFromMetadata, AbilityMetadata}
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** The arcane spell of the Spirit of Nara: a volley of [[ArcaneMissiles.missiles]] missiles at its
  * target, the longest to cast and the most dangerous of its spells.
  */
final case class ArcaneMissiles(
    useId: Ability.UseId,
    time: Long,
    casterId: Entity.Id,
    targetId: Entity.Id
) extends NaraNuke
    with AbilityInfoFromMetadata[ArcaneMissiles.type] {
  def metadata = ArcaneMissiles

  def school: Int = MagicSchool.arcane

  def cost: Resource.ResourceAmount =
    Resource.ResourceAmount(ArcaneMissiles.manaCost, Resource.Mana)

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    Vector.fill(ArcaneMissiles.missiles)(
      EntityTakesDamage(genActionId(), time, targetId, ArcaneMissiles.damagePerMissile, casterId)
    )

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): ArcaneMissiles =
    copy(time = newTime, useId = newId)
}

object ArcaneMissiles extends AbilityMetadata {
  val name: String             = "Arcane Missiles"
  val cooldown: Long           = 7000L
  val castingTime: Long        = 3000L
  val timeToFirstAbility: Long = 4000L

  inline def missiles: Int            = 3
  inline def damagePerMissile: Double = 13.2
  inline def manaCost: Double         = 60.0

  def abilityId: AbilityId = Ability.spiritOfNaraArcaneMissiles
}
