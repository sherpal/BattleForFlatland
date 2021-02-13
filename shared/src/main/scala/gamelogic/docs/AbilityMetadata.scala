package gamelogic.docs

import gamelogic.abilities.Ability

/**
  * Describe the properties of a boss ability.
  */
trait AbilityMetadata {

  /** Name of the ability (in English) */
  def name: String

  /** Cooldown of the ability in millis */
  def cooldown: Long

  /** Casting time of the ability in millis */
  def castingTime: Long

  /** Time before the boss can use the ability for the first time. */
  def timeToFirstAbility: Long

  /** Unique [[Ability.AbilityId]] of this [[Ability]]. */
  def abilityId: Ability.AbilityId

  /** Computes the time for last use in order for the first ability to be used after the good time to first ability. */
  final def setBeginningOfGameAbilityUse(time: Long): Long = time - cooldown + timeToFirstAbility

}
