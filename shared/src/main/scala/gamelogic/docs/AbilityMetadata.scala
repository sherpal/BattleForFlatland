package gamelogic.docs

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

}
