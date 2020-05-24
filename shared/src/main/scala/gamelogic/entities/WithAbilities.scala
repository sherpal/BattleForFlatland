package gamelogic.entities

import gamelogic.abilities.Ability
import gamelogic.entities.Resource.ResourceAmount

/**
  * A [[WithAbilities]] [[Entity]] have a position because we need to check that they don't move while casting.
  * Of course, something casting but without actual position could "fake" one.
  */
trait WithAbilities extends WithPosition {

  /** List of [[gamelogic.abilities.Ability]] that is entity has. */
  def abilities: Set[Ability.AbilityId]

  /**
    * Copies this [[WithAbilities]] by changing it after using the ability.
    * This would also involve reduce the value `resourceAmount`.
    */
  def useAbility(ability: Ability): WithAbilities

  /** Type of resource, and amount, that this entity has. */
  def resourceAmount: ResourceAmount
  final def resourceType: Resource = resourceAmount.resourceType
  def maxResourceAmount: Double

  /**
    * Updates this [[WithAbilities]] by changing the resource amount.
    *
    * This should just "patch" the value and should not care about the amount being positive or smaller than the
    * maximum.
    *
    * It will most likely always be implemented with `copy(resourceAmount = newResourceAmount)`
    */
  protected def patchResourceAmount(newResourceAmount: ResourceAmount): WithAbilities

  /**
    * Adds the (possibly negative) value to the resource amount, restricting it to [0, maxResourceAmount]
    */
  def resourceAmountChange(delta: ResourceAmount): WithAbilities =
    patchResourceAmount((resourceAmount + delta).clampTo(maxResourceAmount))

  /** Returns whether this entity has the given ability. */
  final def hasAbility(abilityId: Ability.AbilityId): Boolean = abilities.contains(abilityId)

  /**
    * This Map remembers the last usage of each of the abilities that this [[WithAbilities]] has.
    * This probably need a little bit more thought, but the idea is that, in the future of the game, all
    * knowledge about what the next ability can do, or may be used, or anything else.
    */
  val relevantUsedAbilities: Map[Ability.AbilityId, Ability]

  /**
    * Returns whether this [[WithAbilities]] can cast this ability.
    * @param ability ability it wants to use
    * @param now game time now
    */
  final def canUseAbility(ability: Ability, now: Long): Boolean =
    hasAbility(ability.abilityId) &&
      relevantUsedAbilities.get(ability.abilityId).forall { ability =>
        now - ability.time >= ability.cooldown
      } && (resourceAmount >= ability.cost)

}
