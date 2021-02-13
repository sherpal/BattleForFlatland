package gamelogic.docs

import gamelogic.abilities.Ability

trait AbilityInfoFromMetadata[Metadata <: AbilityMetadata] {

  def metadata: Metadata

  final def abilityId: Ability.AbilityId = metadata.abilityId

  final def cooldown: Long = metadata.cooldown

  final def castingTime: Long = metadata.castingTime

}
