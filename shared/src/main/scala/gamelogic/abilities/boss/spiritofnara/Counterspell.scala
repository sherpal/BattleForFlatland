package gamelogic.abilities.boss.spiritofnara

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.docs.{AbilityInfoFromMetadata, AbilityMetadata}
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.EntityCastingInterrupted
import gamelogic.gamestate.gameactions.spiritofnara.PutCounterspelled
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer
import services.i18n.I18n

/** Interrupts every player within [[Counterspell.radius]] of the Spirit of Nara, and keeps them
  * from using mana for a while (see [[gamelogic.buffs.boss.spiritofnara.Counterspelled]]).
  */
final case class Counterspell(useId: Ability.UseId, time: Long, casterId: Entity.Id)
    extends Ability
    with AbilityInfoFromMetadata[Counterspell.type] {
  def metadata = Counterspell

  def cost: Resource.ResourceAmount = Resource.ResourceAmount(0, Resource.Mana)

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    Counterspell
      .victims(gameState, casterId, time)
      .flatMap(playerId =>
        Vector(
          EntityCastingInterrupted(genActionId(), time, playerId),
          PutCounterspelled(genActionId(), time, genBuffId(), playerId)
        )
      )

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Counterspell =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long)(using I18n): None.type = None
}

object Counterspell extends AbilityMetadata {
  val name: String             = "Counterspell"
  val cooldown: Long           = 15000L
  val castingTime: Long        = 0L
  val timeToFirstAbility: Long = 12000L

  inline def radius: Double = 150.0

  def abilityId: AbilityId = Ability.spiritOfNara.counterspellId

  /** The players close enough to the caster to be counterspelled. */
  def victims(gameState: GameState, casterId: Entity.Id, time: Long): Vector[Entity.Id] =
    gameState
      .withPositionEntityById(casterId)
      .fold(Vector.empty[Entity.Id]) { caster =>
        val position = caster.currentPosition(time)
        gameState.players.values.toVector
          .filter(_.currentPosition(time).distanceTo(position) < radius)
          .map(_.id)
          .sortBy(_.value)
      }
}
