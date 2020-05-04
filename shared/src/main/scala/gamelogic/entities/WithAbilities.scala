package gamelogic.entities

import gamelogic.abilities.Ability

/**
  * A [[WithAbilities]] [[Entity]] have a position because we need to check that they don't move while casting.
  * Of course, something casting but without actual position could "fake" one.
  */
trait WithAbilities extends WithPosition {

  /** List of [[gamelogic.abilities.Ability]] that is entity has. */
  val abilities: Set[Ability.AbilityId]

  def useAbility(ability: Ability): WithAbilities

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
    * @param abilityId id of the ability it wants to use
    * @param now game time now
    */
  final def canUseAbility(abilityId: Ability.AbilityId, now: Long): Boolean =
    hasAbility(abilityId) &&
      relevantUsedAbilities.get(abilityId).forall { ability =>
        now - ability.time >= ability.cooldown
      }

}
