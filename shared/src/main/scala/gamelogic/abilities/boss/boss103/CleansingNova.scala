package gamelogic.abilities.boss.boss103

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.docs.{AbilityInfoFromMetadata, AbilityMetadata}
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** Deals a deadly amount of damage to every player in sight of the boss. */
final case class CleansingNova(useId: Ability.UseId, time: Long, casterId: Entity.Id)
    extends Ability
    with AbilityInfoFromMetadata[CleansingNova.type] {
  def metadata = CleansingNova

  def cost: Resource.ResourceAmount = Resource.ResourceAmount(0, Resource.NoResource)

  def createActions(gameState: GameState)(implicit idGeneratorContainer: IdGeneratorContainer): List[GameAction] =
    gameState.players.valuesIterator
      .filter(player => gameState.areTheyInSight(casterId, player.id, time).getOrElse(false))
      .map { player =>
        EntityTakesDamage(idGeneratorContainer.gameActionIdGenerator(), time, player.id, CleansingNova.damage, casterId)
      }
      .toList

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): CleansingNova = copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): None.type = None
}

object CleansingNova extends AbilityMetadata {

  val cooldown: Long    = 60000L
  val castingTime: Long = 4000L

  val damage: Double = 300.0

  val name: String = "CleansingNova"

  val timeToFirstAbility: Long = 30000L

  def abilityId: Ability.AbilityId = Ability.boss103CleansingNovaId

}
