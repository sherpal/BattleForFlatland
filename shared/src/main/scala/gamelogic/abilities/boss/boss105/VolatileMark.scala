package gamelogic.abilities.boss.boss105

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.buffs.boss.boss105.VolatileCharge
import gamelogic.docs.{AbilityInfoFromMetadata, AbilityMetadata}
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.boss105.PutVolatileCharge
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

import scala.util.Random

/** Puts a [[VolatileCharge]] on random players. */
final case class VolatileMark(useId: Ability.UseId, time: Long, casterId: Entity.Id)
    extends Ability
    with AbilityInfoFromMetadata[VolatileMark.type] {
  def metadata = VolatileMark

  def cost: Resource.ResourceAmount = Resource.noResourceAmount

  /** The bearers are chosen here, once, on the game master: the actions carry the decision. */
  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] = {
    val candidates = gameState.players.values.toVector
      .filterNot(player => gameState.hasBuffOfType[VolatileCharge](player.id))

    Random
      .shuffle(candidates)
      .take(VolatileMark.numberOfBearers)
      .map(player => PutVolatileCharge(genActionId(), time, genBuffId(), player.id, casterId))
  }

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): VolatileMark =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): None.type = None
}

object VolatileMark extends AbilityMetadata {

  val cooldown: Long    = 22000L
  val castingTime: Long = 1500L

  val timeToFirstAbility: Long = 7000L

  def name: String = "Volatile Mark"

  def abilityId: AbilityId = Ability.boss105VolatileMark

  inline def numberOfBearers: Int = 2
}
