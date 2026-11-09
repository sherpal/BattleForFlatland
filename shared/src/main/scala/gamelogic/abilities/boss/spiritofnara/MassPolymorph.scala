package gamelogic.abilities.boss.spiritofnara

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.docs.{AbilityInfoFromMetadata, AbilityMetadata}
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.EntityCastingInterrupted
import gamelogic.gamestate.gameactions.spiritofnara.{PutDrinking, PutPolymorph}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer
import services.i18n.I18n

/** When it runs out of mana, the Spirit of Nara polymorphs every player (see
  * [[gamelogic.buffs.boss.spiritofnara.Polymorphed]]) and drinks (see
  * [[gamelogic.buffs.boss.spiritofnara.Drinking]]). A [[Pyroblast]] follows.
  */
final case class MassPolymorph(useId: Ability.UseId, time: Long, casterId: Entity.Id)
    extends Ability
    with AbilityInfoFromMetadata[MassPolymorph.type] {
  def metadata = MassPolymorph

  def cost: Resource.ResourceAmount = Resource.ResourceAmount(0, Resource.Mana)

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    gameState.players.values.toVector.sortBy(_.id.value).flatMap { player =>
      Vector(
        EntityCastingInterrupted(genActionId(), time, player.id),
        PutPolymorph(
          genActionId(),
          time,
          genBuffId(),
          player.id,
          casterId,
          player.currentPosition(time)
        )
      )
    } :+ PutDrinking(genActionId(), time, genBuffId(), casterId)

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): MassPolymorph =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long)(using I18n): None.type = None
}

object MassPolymorph extends AbilityMetadata {
  val name: String             = "Mass Polymorph"
  val cooldown: Long           = 30000L
  val castingTime: Long        = 2000L
  val timeToFirstAbility: Long = 0L

  /** Below this fraction of its mana, the Spirit of Nara needs a drink. */
  inline def manaThreshold: Double = 0.2

  def abilityId: AbilityId = Ability.spiritOfNara.massPolymorphId
}
