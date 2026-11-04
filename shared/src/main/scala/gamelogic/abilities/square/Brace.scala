package gamelogic.abilities.square

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.buffs.abilities.classes.BraceShield
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.PosInt
import gamelogic.utils.IdGeneratorContainer

/** Adds a stack to the caster's [[gamelogic.buffs.abilities.classes.BraceShield]], up to
  * `BraceShield.maxStacks`. When the shield is already full, its remaining time is simply reset.
  */
final case class Brace(useId: Ability.UseId, time: Long, casterId: Entity.Id) extends Ability {
  def abilityId: AbilityId = Ability.squareBraceId

  def cooldown: Long = Brace.cooldown

  def castingTime: Long = 0L

  def cost: Resource.ResourceAmount = Brace.cost

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] = {
    val newStacks =
      BraceShield.stacksOn(gameState, casterId).fold(PosInt.of(1))(_ + PosInt.of(1))
    BraceShield.setStacks(gameState, casterId, time, Some(newStacks))
  }

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): None.type = None
}

object Brace {

  inline def cooldown: Long                = 1000L
  inline def cost: Resource.ResourceAmount = Resource.ResourceAmount(15.0, Resource.Rage)

}
