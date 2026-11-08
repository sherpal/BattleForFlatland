package gamelogic.docs

import gamelogic.abilities.Ability

final class AbilityDocSpecs extends munit.FunSuite {

  test("Every ability has a name, for the statistics of the fight") {
    val unnamed = (1 to Ability.abilityIdCount.value)
      .map(Ability.AbilityId.fromInt)
      .filterNot(_ == Ability.simpleBulletId) // not used anymore
      .filterNot(AbilityDoc.names.contains)
    assertEquals(unnamed, Vector.empty)
  }

}
