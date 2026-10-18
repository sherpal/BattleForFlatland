package gamelogic.docs

import gamelogic.entities.Entity
import gamelogic.entities.boss.BossFactory
import models.bff.outofgame.PlayerClasses

/** The docs are what players read in the lobby before a fight: an ability missing from them is an
  * ability players discover the hard way.
  */
final class GameDocsSpecs extends munit.FunSuite {

  test("Every class documents exactly its abilities") {
    PlayerClasses.allChoices.foreach { cls =>
      assertEquals(
        cls.builder.abilitiesDoc.map(_.abilityId).toSet,
        cls.builder.abilities,
        s"Documented abilities of ${cls.value} don't match its abilities"
      )
    }
  }

  test("Every boss documents all of its abilities") {
    BossFactory.factoriesByBossName.values.foreach { factory =>
      val metadata = BossMetadata.maybeMetadataByName(factory.name)
      assert(metadata.isDefined, s"${factory.name} has no BossMetadata")
      val documented = metadata.get.abilitiesDoc.filter(_.caster.isEmpty).map(_.abilityId).toSet
      val abilities  = factory.initialBoss(Entity.Id.zero, 0L).abilities
      assertEquals(abilities -- documented, Set.empty, s"Undocumented abilities of ${factory.name}")
    }
  }

}
