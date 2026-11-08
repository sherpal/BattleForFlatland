package gamelogic.gamestate.abilitiesstories

import gamelogic.abilities.Ability
import gamelogic.abilities.square.{Brace, Guard}
import gamelogic.buffs.abilities.classes.{Guarded, Guarding}
import gamelogic.entities.Entity
import gamelogic.entities.Resource.Rage
import gamelogic.entities.classes.Square
import gamelogic.gamestate.GameState
import gamelogic.gamestate.gameactions.{
  AddPlayerByClass,
  EntityResourceChanges,
  EntityTakesDamage,
  RemoveEntity
}
import models.bff.outofgame.PlayerClasses
import testutils.ActionComposer

final class SquareGuardSpecs extends StoryTeller {

  private val addingSquare = AddPlayerByClass(
    genActionId(),
    2,
    genEntityId(),
    0,
    PlayerClasses.Square,
    0,
    "TheSquare"
  )
  private val squareId: Entity.Id = addingSquare.entityId

  private val addingHexagon         = addHexagon(2)
  private val hexagonId: Entity.Id  = addingHexagon.entityId
  private val addingTriangle        = addPlayer(2)
  private val triangleId: Entity.Id = addingTriangle.entityId
  private val addingHound           = addHound(2)
  private val houndId: Entity.Id    = addingHound.entityId

  /** The square (with its starting buffs, but no rage) and its allies, which has braced `braces`
    * times.
    */
  private def situation(braces: Int = 0): GameState =
    (1 to braces).foldLeft(
      ActionComposer.empty >>
        start >>
        addingSquare >>
        addingHexagon >>
        addingTriangle >>
        addingHound >>>
        (_ => Square.startingActions(2, squareId)) >>
        EntityResourceChanges(genActionId(), 3, squareId, -1000.0, Rage)
    ) { (composer, i) =>
      val ability = Brace(nextUseId(), 10L * i, squareId)
      composer >>> (gs => ability.createActions(gs))
    }(initialGameState)

  private def guard(time: Long, targetId: Entity.Id) =
    Guard(nextUseId(), time, squareId, targetId)

  private def guarded(gameState: GameState, targetId: Entity.Id): GameState = {
    val ability = guard(50, targetId)
    gameState.applyActions(ability.createActions(gameState))
  }

  private def hit(time: Long, targetId: Entity.Id, amount: Double) =
    EntityTakesDamage(genActionId(), time, targetId, amount, houndId, Ability.autoAttackId)

  private def lifeOf(gameState: GameState, id: Entity.Id): Double =
    gameState.livingEntityById(id).get.life

  private def guardings(gameState: GameState): Vector[Guarding] =
    gameState.allTBuffs[Guarding].toVector

  private def guardeds(gameState: GameState): Vector[Guarded] =
    gameState.allTBuffs[Guarded].toVector

  /** What the server does for the buffs of a player that died: their dying actions, then the player
    * is removed.
    */
  private def dies(gameState: GameState, id: Entity.Id): GameState = {
    val dying = gameState.allBuffsOfEntity(id).toVector.flatMap(_.bearerDiedAction(gameState, 100))
    gameState.applyActions(dying :+ RemoveEntity(genActionId(), 100, id))
  }

  private def rageOf(gameState: GameState): Double =
    gameState.withAbilityEntitiesById(squareId).get.resourceAmount.amount

  test("Guard can only target another ally") {
    val gameState = situation()
    assertEquals(guard(60, hexagonId).canBeCast(gameState, 60), None)
    assertEquals(guard(60, squareId).canBeCast(gameState, 60), Some("You can't guard yourself"))
    assertEquals(guard(60, houndId).canBeCast(gameState, 60), Some("Not an ally"))
  }

  test("Guard puts a Guarding buff on the caster, and a Guarded one on the ally") {
    val gameState = guarded(situation(), hexagonId)

    assertEquals(
      guardings(gameState).map(g => (g.bearerId, g.guardedId)),
      Vector(squareId -> hexagonId)
    )
    assertEquals(
      guardeds(gameState).map(g => (g.bearerId, g.sourceId)),
      Vector(hexagonId -> squareId)
    )
  }

  test("Each of the two buffs knows the other one") {
    val gameState    = guarded(situation(), hexagonId)
    val guardingBuff = guardings(gameState).head
    val guardedBuff  = guardeds(gameState).head
    assertEquals(guardingBuff.guardedBuffId, guardedBuff.buffId)
    assertEquals(guardedBuff.guardingBuffId, guardingBuff.buffId)
  }

  test("The guarded ally takes less damage, and the square takes the difference") {
    val before = guarded(situation(), hexagonId)
    val after  = hit(100, hexagonId, 40)(before)

    val share = Guarding.redirectedShare
    assertEqualsDouble(lifeOf(before, hexagonId) - lifeOf(after, hexagonId), 40 * (1 - share), 1e-9)
    assertEqualsDouble(lifeOf(before, squareId) - lifeOf(after, squareId), 40 * share, 1e-9)
  }

  test("The damage taken by the others, and by the square, is not changed") {
    val before = guarded(situation(), hexagonId)
    val after  = hit(100, triangleId, 30)(hit(100, squareId, 20)(before))
    assertEqualsDouble(lifeOf(before, triangleId) - lifeOf(after, triangleId), 30, 1e-9)
    assertEqualsDouble(lifeOf(before, squareId) - lifeOf(after, squareId), 20, 1e-9)
  }

  test("Without Guard, nothing is redirected") {
    val before = situation()
    val after  = hit(100, hexagonId, 40)(before)
    assertEqualsDouble(lifeOf(before, hexagonId) - lifeOf(after, hexagonId), 40, 1e-9)
    assertEqualsDouble(lifeOf(before, squareId), lifeOf(after, squareId), 1e-9)
  }

  test("What the square takes is reduced by its shield, and it gets the rage of the full amount") {
    val damage = 40.0
    val share  = Guarding.redirectedShare

    val withoutShield = hit(100, hexagonId, damage)(guarded(situation(0), hexagonId))
    val withShield    = hit(100, hexagonId, damage)(guarded(situation(3), hexagonId))

    // the shield reduces what the square takes...
    val lostWithout =
      lifeOf(guarded(situation(0), hexagonId), squareId) - lifeOf(withoutShield, squareId)
    val lostWith = lifeOf(guarded(situation(3), hexagonId), squareId) - lifeOf(withShield, squareId)
    assertEqualsDouble(lostWithout, damage * share, 1e-9)
    assertEqualsDouble(lostWith, damage * share * math.pow(0.95, 3), 1e-9)

    // ... but not the rage that it gets
    assertEqualsDouble(rageOf(withoutShield), damage * share, 1e-9)
    assertEqualsDouble(rageOf(withShield), damage * share, 1e-9)
  }

  test("The buffs end together, after the duration") {
    val gameState    = guarded(situation(), hexagonId)
    val guardingBuff = guardings(gameState).head
    val guardedBuff  = guardeds(gameState).head
    assert(guardingBuff.isFinite)
    assertEquals(guardingBuff.duration, Guarding.duration)
    assertEquals(guardedBuff.duration, Guarding.duration)
    assertEquals(guardedBuff.appearanceTime, guardingBuff.appearanceTime)
  }

  test("The guard is over before the cooldown, so that there is a single one at a time") {
    assert(Guard.cooldown > Guarding.duration)
  }

  test("When the square dies, its ally is not shown as guarded anymore") {
    val gameState = dies(guarded(situation(), hexagonId), squareId)
    assertEquals(guardings(gameState), Vector.empty)
    assertEquals(guardeds(gameState), Vector.empty)
  }

  test("When the ally dies, the square does not guard anyone anymore") {
    val gameState = dies(guarded(situation(), hexagonId), hexagonId)
    assertEquals(guardings(gameState), Vector.empty)
    assertEquals(guardeds(gameState), Vector.empty)
  }

}
