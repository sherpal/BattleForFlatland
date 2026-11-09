package gamelogic.gameextras

import gamelogic.abilities.Ability
import gamelogic.buffs.Buff
import gamelogic.buffs.abilities.classes.Facets
import gamelogic.entities.Entity
import gamelogic.entities.boss.boss102.BossHound
import gamelogic.entities.classes.Square
import gamelogic.gameextras.FightLog.{Key, LifeChange, Totals}
import gamelogic.gamestate.GameState
import gamelogic.gamestate.abilitiesstories.StoryTeller
import gamelogic.gamestate.gameactions.*
import gamelogic.physics.PosInt
import models.bff.outofgame.PlayerClasses
import testutils.ActionComposer

final class FightLogSpecs extends StoryTeller {

  private def addPlayer(name: String, cls: PlayerClasses) =
    AddPlayerByClass(genActionId(), 0, genEntityId(), 0, cls, 0, name)

  private val addingFirst    = addPlayer("First", PlayerClasses.Square)
  private val addingSecond   = addPlayer("Second", PlayerClasses.Square)
  private val addingPentagon = addPlayer("Pentagon", PlayerClasses.Pentagon)
  private val addingHound    = addHound(2)
  private val addingBoss     = addBoss101(2)

  private val first: Entity.Id    = addingFirst.entityId
  private val second: Entity.Id   = addingSecond.entityId
  private val pentagon: Entity.Id = addingPentagon.entityId
  private val hound: Entity.Id    = addingHound.entityId
  private val boss: Entity.Id     = addingBoss.entityId

  private val gameState: GameState =
    (ActionComposer.empty >> addingFirst >> addingSecond >> addingPentagon >> start >>
      addingHound >> addingBoss)(initialGameState)

  private def hit(
      time: Long,
      target: Entity.Id,
      amount: Double,
      source: Entity.Id,
      ability: Ability.AbilityId = Ability.square.hammerHitId
  ) = EntityTakesDamage(genActionId(), time, target, amount, source, ability)

  private def heal(time: Long, target: Entity.Id, amount: Double, source: Entity.Id) =
    EntityGetsHealed(genActionId(), time, target, amount, source, Ability.hexagon.flashHealId)

  private def braceStacks(time: Long, square: Entity.Id, stacks: Int) =
    PutStackedBuff(
      genActionId(),
      time,
      genBuffId(),
      square,
      square,
      time,
      Buff.squareBrace,
      PosInt.maybe(stacks).get
    )

  test("The damage of each player to an enemy is added, the highest first") {
    val log = new FightLog
    log.received(
      Vector(hit(10, boss, 30, first), hit(20, boss, 50, second), hit(30, boss, 40, first)),
      gameState
    )
    assertEquals(log.damageTo(boss), Vector(first -> 70.0, second -> 50.0))
  }

  test("The damage is counted separately for each enemy") {
    val log = new FightLog
    log.received(Vector(hit(10, boss, 30, first), hit(20, hound, 5, first)), gameState)
    assertEquals(log.damageTo(boss), Vector(first -> 30.0))
    assertEquals(log.damageTo(hound), Vector(first -> 5.0))
  }

  test("An action sent again by the server is counted once") {
    val log    = new FightLog
    val action = hit(10, boss, 30, first)
    log.received(Vector(action), gameState)
    log.received(Vector(action), gameState)
    assertEquals(log.damageTo(boss), Vector(first -> 30.0))
  }

  test("Only what the players deal to the others counts in the damage meter") {
    val log = new FightLog
    log.received(
      Vector(
        hit(10, first, 20, boss), // taken by a player
        hit(20, first, 5, first), // self inflicted (like Enrage)
        hit(30, second, 10, first) // between players
      ),
      gameState
    )
    assertEquals(log.damageTo(boss), Vector.empty)
    assertEquals(log.damageTo(first), Vector.empty)
    assertEquals(log.damageTo(second), Vector.empty)
  }

  test("The first and last times that an enemy was hit are known") {
    val log = new FightLog
    assertEquals(log.hitTimes(boss), None)
    log.received(Vector(hit(30, boss, 1, first), hit(10, boss, 1, second)), gameState)
    log.received(Vector(hit(20, boss, 1, first)), gameState)
    assertEquals(log.hitTimes(boss), Some((10L, 30L)))
  }

  test("The received actions are applied to the game state") {
    val log   = new FightLog
    val after = log.received(Vector(hit(10, hound, 30, first)), gameState)
    assertEquals(after.livingEntityById(hound).map(_.life), Some(BossHound.houndMaxLife - 30))
  }

  test("The damage is counted by source, target and ability, without the overkill") {
    val log = new FightLog
    log.received(
      Vector(
        hit(10, hound, 300, first),
        hit(20, hound, 300, first, Ability.square.cleaveId),
        hit(30, hound, 300, first) // only 200 life left
      ),
      gameState
    )
    assertEquals(
      log.damageEntries.map(e => e.key -> e.totals).toMap,
      Map(
        Key(first, hound, Ability.square.hammerHitId) -> Totals(2, 600, 500),
        Key(first, hound, Ability.square.cleaveId)    -> Totals(1, 300, 300)
      )
    )
    assertEquals(log.nameOf(first), Some("First"))
    assertEquals(log.nameOf(hound), Some(BossHound.name)) // the adds have a name too
  }

  test("The heals are counted without the overheal") {
    val log = new FightLog
    log.received(Vector(hit(10, first, 30, boss), heal(20, first, 50, second)), gameState)
    assertEquals(
      log.healEntries.map(e => e.key -> e.totals),
      Vector(Key(second, first, Ability.hexagon.flashHealId) -> Totals(1, 50, 30))
    )
  }

  test("A death comes with the life changes of the player in the seconds before it") {
    val log = new FightLog
    log.received(
      Vector(
        hit(1000, first, 50, boss), // too long before the death
        hit(7000, first, 100, boss, Ability.boss101.bigHitId),
        heal(8000, first, 20, second),
        hit(9000, first, 100, boss, Ability.boss101.smallHitId),
        RemoveEntity(genActionId(), 9000, first)
      ),
      gameState
    )
    val maxLife = Square.initialMaxLife
    assertEquals(log.deaths.map(d => d.playerId -> d.time), Vector(first -> 9000L))
    assertEquals(
      log.deaths.head.recap,
      Vector(
        LifeChange(7000, boss, Ability.boss101.bigHitId, 100, 100, false, maxLife - 150),
        LifeChange(8000, second, Ability.hexagon.flashHealId, 20, 20, true, maxLife - 130),
        LifeChange(9000, boss, Ability.boss101.smallHitId, 100, 70, false, 0)
      )
    )
    assertEquals(log.recapOf(first, 20000), log.deaths.head.recap)
  }

  test("A player alive has its last life changes as recap") {
    val log = new FightLog
    log.received(Vector(hit(1000, first, 10, boss), hit(5000, first, 20, boss)), gameState)
    assertEquals(log.recapOf(first, 7000).map(_.time), Vector(5000L))
  }

  test("The bullets of a pentagon hitting while its facets are all lit are wasted") {
    val log                = new FightLog
    def bullet(time: Long) = hit(time, boss, 10, pentagon, Ability.pentagon.pentagonBulletId)
    val withFourFacets =
      Facets
        .setStacks(gameState, pentagon, 5, Some(PosInt.of(4)))
        .foldLeft(gameState)((gs, a) => a(gs))
    // Two bullets at once: the first lights the last facet, the second is wasted
    val lit = log.received(
      Vector(bullet(10), bullet(10)) ++
        Facets.setStacks(withFourFacets, pentagon, 10, Some(PosInt.of(6))),
      withFourFacets
    )
    assertEquals(log.facetsWastedBy(pentagon), 1)
    log.received(Vector(bullet(20)), lit)
    assertEquals(log.facetsWastedBy(pentagon), 2)
  }

  test("The time spent by a square with each number of brace stacks is known") {
    val log    = new FightLog
    val before = (ActionComposer.empty >> addingFirst)(initialGameState)
    log.received(
      Vector(
        GameStart(genActionId(), 1000),
        braceStacks(3000, first, 1),
        braceStacks(4000, first, 5),
        EndGame(genActionId(), 10000)
      ),
      before
    )
    assertEquals(log.braceTimeOf(first, 20000), Vector(2000L, 1000L, 0L, 0L, 0L, 6000L))
  }

  test("The time of a square with brace stacks goes on until now during the fight") {
    val log    = new FightLog
    val before = (ActionComposer.empty >> addingFirst)(initialGameState)
    log.received(Vector(GameStart(genActionId(), 1000), braceStacks(3000, first, 2)), before)
    assertEquals(log.braceTimeOf(first, 4000), Vector(2000L, 0L, 1000L, 0L, 0L, 0L))
  }

}
