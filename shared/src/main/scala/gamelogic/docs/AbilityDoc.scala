package gamelogic.docs

import gamelogic.abilities.Ability
import gamelogic.entities.Resource
import gamelogic.entities.Resource.ResourceAmount

/** Player-facing description of an ability, displayed in the lobby so that players know what their
  * class and the boss they are about to fight can do.
  *
  * Descriptions should be built from the constants used by the game logic (instead of hard-coded
  * numbers), so that they stay accurate when the balance changes.
  *
  * @param abilityId
  *   id of the described ability (used, for example, to retrieve its icon)
  * @param name
  *   name of the ability (in English)
  * @param description
  *   what the ability does (in English)
  * @param cooldown
  *   cooldown of the ability, in millis
  * @param castingTime
  *   casting time of the ability, in millis
  * @param cost
  *   resource cost of the ability
  * @param caster
  *   name of the entity using the ability, when it's not the main entity being described (typically,
  *   an add of a boss)
  */
final case class AbilityDoc(
    abilityId: Ability.AbilityId,
    name: String,
    description: String,
    cooldown: Long,
    castingTime: Long,
    cost: ResourceAmount = Resource.noResourceAmount,
    caster: Option[String] = None
) {

  def castBy(casterName: String): AbilityDoc = copy(caster = Some(casterName))

  def cooldownRepr: String = if cooldown <= Ability.gcd then "None" else formatSeconds(cooldown)

  def castingTimeRepr: String = if castingTime == 0L then "Instant" else formatSeconds(castingTime)

  def maybeCostRepr: Option[String] =
    Option.when(cost.resourceType != Resource.NoResource && cost.amount > 0)(
      s"${formatNumber(cost.amount)} ${cost.resourceType.toString.toLowerCase}"
    )

}

object AbilityDoc {

  /** Creates the [[AbilityDoc]] of a boss ability documented with an [[AbilityMetadata]]. */
  def fromMetadata(metadata: AbilityMetadata, description: String): AbilityDoc =
    AbilityDoc(
      metadata.abilityId,
      metadata.name,
      description,
      metadata.cooldown,
      metadata.castingTime
    )

}
