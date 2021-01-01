package gamelogic.abilities.boss.boss103

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.docs.AbilityMetadata
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.EntityCastingInterrupted
import gamelogic.gamestate.gameactions.boss103.PutPunishedDebuff
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/**
  * Curse all players with the [[gamelogic.buffs.boss.boss103.Punished]] keeping them from doing anything for some time.
  */
final case class Punishment(useId: Ability.UseId, time: Long, casterId: Entity.Id) extends Ability {
  def abilityId: AbilityId = Ability.boss103PunishmentId

  def cooldown: Long = Punishment.cooldown

  def castingTime: Long = Punishment.castingTime

  def cost: Resource.ResourceAmount = Resource.ResourceAmount(0, Resource.NoResource)

  def createActions(gameState: GameState)(implicit idGeneratorContainer: IdGeneratorContainer): List[GameAction] =
    gameState.players.valuesIterator.flatMap { player =>
      List(
        EntityCastingInterrupted(id = idGeneratorContainer.gameActionIdGenerator(), time = time, entityId = player.id),
        PutPunishedDebuff(
          idGeneratorContainer.gameActionIdGenerator(),
          time,
          idGeneratorContainer.buffIdGenerator(),
          player.id,
          casterId
        )
      )
    }.toList

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Punishment = copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): None.type = None
}

object Punishment extends AbilityMetadata {

  val cooldown    = 20000L
  val castingTime = 1500L

  val timeToFirstAbility = 20000L

  def name: String = "Punishment"
}
