package gamelogic.gameextras

import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import gamelogic.gamestate.abilitiesstories.StoryTeller
import gamelogic.gamestate.gameactions.{AddPlayerByClass, EntityTakesDamage}
import models.bff.outofgame.PlayerClasses
import testutils.ActionComposer

final class DamageTrackerSpecs extends StoryTeller {

  private def addSquare(name: String) =
    AddPlayerByClass(genActionId(), 2, genEntityId(), 0, PlayerClasses.Square, 0, name)

  private val addingFirst  = addSquare("First")
  private val addingSecond = addSquare("Second")
  private val addingHound  = addHound(2)
  private val addingBoss   = addBoss101(2)

  private val first: Entity.Id  = addingFirst.entityId
  private val second: Entity.Id = addingSecond.entityId
  private val hound: Entity.Id  = addingHound.entityId
  private val boss: Entity.Id   = addingBoss.entityId

  private val gameState: GameState =
    (ActionComposer.empty >> start >> addingFirst >> addingSecond >> addingHound >> addingBoss)(
      initialGameState
    )

  private def hit(time: Long, target: Entity.Id, amount: Double, source: Entity.Id) =
    EntityTakesDamage(genActionId(), time, target, amount, source)

  test("The damage of each player to an enemy is added, the highest first") {
    val tracker = new DamageTracker
    tracker.received(
      Vector(hit(10, boss, 30, first), hit(20, boss, 50, second), hit(30, boss, 40, first)),
      gameState
    )
    assertEquals(tracker.damageTo(boss), Vector(first -> 70.0, second -> 50.0))
  }

  test("The damage is counted separately for each enemy") {
    val tracker = new DamageTracker
    tracker.received(Vector(hit(10, boss, 30, first), hit(20, hound, 5, first)), gameState)
    assertEquals(tracker.damageTo(boss), Vector(first -> 30.0))
    assertEquals(tracker.damageTo(hound), Vector(first -> 5.0))
  }

  test("An action sent again by the server is counted once") {
    val tracker = new DamageTracker
    val action  = hit(10, boss, 30, first)
    tracker.received(Vector(action), gameState)
    tracker.received(Vector(action), gameState)
    assertEquals(tracker.damageTo(boss), Vector(first -> 30.0))
  }

  test("Only what the players deal to the others counts") {
    val tracker = new DamageTracker
    tracker.received(
      Vector(
        hit(10, first, 20, boss), // taken by a player
        hit(20, first, 5, first), // self inflicted (like Enrage)
        hit(30, second, 10, first) // between players
      ),
      gameState
    )
    assertEquals(tracker.damageTo(boss), Vector.empty)
    assertEquals(tracker.damageTo(first), Vector.empty)
    assertEquals(tracker.damageTo(second), Vector.empty)
  }

  test("The first and last times that an enemy was hit are known") {
    val tracker = new DamageTracker
    assertEquals(tracker.hitTimes(boss), None)
    tracker.received(Vector(hit(30, boss, 1, first), hit(10, boss, 1, second)), gameState)
    tracker.received(Vector(hit(20, boss, 1, first)), gameState)
    assertEquals(tracker.hitTimes(boss), Some((10L, 30L)))
  }

}
