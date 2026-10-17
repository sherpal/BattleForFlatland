package gamelogic.abilities.boss.sirofvice

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.docs.{AbilityInfoFromMetadata, AbilityMetadata}
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.EntityCastingInterrupted
import gamelogic.gamestate.gameactions.sirofvice.PutPunishedDebuff
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** Interrupts all players, and puts on them the [[gamelogic.buffs.boss.sirofvice.Punished]] debuff,
  * keeping them from doing anything until they take damage.
  */
final case class Punishment(useId: Ability.UseId, time: Long, casterId: Entity.Id)
    extends Ability
    with AbilityInfoFromMetadata[Punishment.type] {
  def metadata = Punishment

  def cost: Resource.ResourceAmount = Resource.ResourceAmount(0, Resource.NoResource)

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    gameState.players.valuesIterator.flatMap { player =>
      Vector(
        EntityCastingInterrupted(id = genActionId(), time = time, entityId = player.id),
        PutPunishedDebuff(
          genActionId(),
          time,
          genBuffId(),
          player.id,
          casterId,
          player.currentPosition(time)
        )
      )
    }.toVector

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Punishment =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): None.type = None
}

object Punishment extends AbilityMetadata {

  val cooldown: Long    = 30000L
  val castingTime: Long = 1500L

  val timeToFirstAbility: Long = 20000L

  def name: String = "Punishment"

  def abilityId: AbilityId = Ability.sirOfVicePunishment
}
