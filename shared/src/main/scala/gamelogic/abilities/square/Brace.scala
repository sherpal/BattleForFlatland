package gamelogic.abilities.square

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.buffs.Buff
import gamelogic.buffs.abilities.classes.BraceShield
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.{PutStackedBuff, RemoveBuff}
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
    // Normally at most one, but if two shields ever coexist, the biggest one wins.
    val currentShields = BraceShield.on(gameState, casterId)
    val newStacks =
      currentShields
        .map(_.stacks)
        .maxOption
        .map(_ + PosInt.of(1))
        .getOrElse(PosInt.of(1))
        .min(BraceShield.maxStacks)

    currentShields.map(shield => RemoveBuff(genActionId(), time, casterId, shield.buffId)) :+
      PutStackedBuff(
        genActionId(),
        time,
        genBuffId(),
        casterId,
        casterId,
        time,
        Buff.squareBrace,
        newStacks
      )
  }

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): None.type = None
}

object Brace {

  inline def cooldown: Long                = 1000L
  inline def cost: Resource.ResourceAmount = Resource.ResourceAmount(15.0, Resource.Rage)

}
