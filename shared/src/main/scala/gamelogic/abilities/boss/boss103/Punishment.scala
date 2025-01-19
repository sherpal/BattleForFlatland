package gamelogic.abilities.boss.boss103

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.docs.{AbilityInfoFromMetadata, AbilityMetadata}
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.EntityCastingInterrupted
import gamelogic.gamestate.gameactions.boss103.PutPunishedDebuff
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** Curse all players with the [[gamelogic.buffs.boss.boss103.Punished]] keeping them from doing
  * anything for some time.
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
          casterId
        )
      )
    }.toVector

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Punishment =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): None.type = None
}

object Punishment extends AbilityMetadata {

  val cooldown    = 20000L
  val castingTime = 1500L

  val timeToFirstAbility = 20000L

  def name: String = "Punishment"

  def abilityId: AbilityId = Ability.boss103PunishmentId
}
