package gamelogic.abilities.boss.boss105

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.docs.{AbilityInfoFromMetadata, AbilityMetadata}
import gamelogic.entities.boss.boss105.Shade
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** The twins resonate when they are too close to each other: every player takes damage, with
  * nothing to do about it but pulling the twins apart.
  *
  * It can only be cast while the [[Shade]] stands within [[Resonance.range]] of the caster, and the
  * AI of the boss uses it each time it is available.
  */
final case class Resonance(useId: Ability.UseId, time: Long, casterId: Entity.Id)
    extends Ability
    with AbilityInfoFromMetadata[Resonance.type] {
  def metadata = Resonance

  def cost: Resource.ResourceAmount = Resource.noResourceAmount

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    gameState.players.valuesIterator
      .map(player => EntityTakesDamage(genActionId(), time, player.id, Resonance.damage, casterId))
      .toVector

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Resonance =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): Option[String] =
    Option.unless(Resonance.twinsAreResonating(gameState, casterId, time))(
      "The twins are far enough from each other"
    )
}

object Resonance extends AbilityMetadata {

  val cooldown: Long    = 1500L
  val castingTime: Long = 0L

  val timeToFirstAbility: Long = 0L

  def name: String = "Resonance"

  def abilityId: AbilityId = Ability.boss105Resonance

  /** Distance between the centres of the twins below which they resonate. */
  inline def range: Double = 280.0

  inline def damage: Double = 10.0

  /** Whether the [[Shade]] is close enough to the boss with id `bossId` to resonate with it. */
  def twinsAreResonating(gameState: GameState, bossId: Entity.Id, time: Long): Boolean =
    (for {
      boss  <- gameState.withPositionEntityById(bossId)
      shade <- gameState.allTEntities[Shade].values.headOption
    } yield boss.currentPosition(time).distanceTo(shade.currentPosition(time)) < range)
      .getOrElse(false)
}
