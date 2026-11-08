package gamelogic.gamestate.abilitiesstories

import gamelogic.abilities.Ability
import gamelogic.abilities.square.Brace
import gamelogic.buffs.Buff
import gamelogic.buffs.abilities.classes.BraceShield
import gamelogic.entities.Entity
import gamelogic.entities.classes.Square
import gamelogic.gamestate.GameState
import gamelogic.gamestate.gameactions.{
  AddPlayerByClass,
  EntityTakesDamage,
  PutStackedBuff,
  RemoveBuff
}
import gamelogic.physics.PosInt
import models.bff.outofgame.PlayerClasses
import testutils.ActionComposer

final class SquareBraceSpecs extends StoryTeller {

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

  private def brace(time: Long) = Brace(nextUseId(), time, squareId)

  private def stacksAt(gameState: GameState): Int =
    BraceShield.on(gameState, squareId).map(_.stacks.toInt).sum

  /** Uses Brace at each of the given times, and returns the last game state. */
  private def bracesAt(times: Long*): GameState =
    times.foldLeft(ActionComposer.empty >> start >> addingSquare) { (composer, time) =>
      val ability = brace(time)
      composer >>> (gs => ability.createActions(gs))
    }(initialGameState)

  test("Without Brace, the square has no shield") {
    assertEquals(stacksAt(bracesAt()), 0)
  }

  test("Each Brace adds a stack, and there is always a single shield") {
    val gameState = bracesAt(10, 20, 30)
    assertEquals(stacksAt(gameState), 3)
    assertEquals(BraceShield.on(gameState, squareId).length, 1)
  }

  test("Brace at the maximum number of stacks keeps it and resets the time") {
    val gameState = bracesAt((1L to BraceShield.maxStacks.toLong + 2).map(_ * 10)*)
    val shields   = BraceShield.on(gameState, squareId)
    assertEquals(shields.map(_.stacks), Vector(BraceShield.maxStacks))
    assertEquals(shields.head.appearanceTime, (BraceShield.maxStacks + PosInt.of(2)).toInt * 10L)
  }

  test("Each stack reduces the damage taken") {
    val damage = 40.0
    val hit =
      EntityTakesDamage(genActionId(), 100, squareId, damage, genEntityId(), Ability.autoAttackId)

    def lifeLostAfterBraces(braces: Int): Double = {
      val before = bracesAt((1L to braces.toLong).map(_ * 10)*)
      val after  = hit(before)
      Square.initialMaxLife - after.livingEntityById(squareId).get.life
    }

    assertEqualsDouble(lifeLostAfterBraces(0), damage, 1e-9)
    assertEqualsDouble(lifeLostAfterBraces(1), damage * 0.95, 1e-9)
    assertEqualsDouble(lifeLostAfterBraces(3), damage * math.pow(0.95, 3), 1e-9)
  }

  test("When a shield ends, it is replaced by one with a stack less, as it ends") {
    val shield = BraceShield(
      genBuffId(),
      squareId,
      squareId,
      appearanceTime = 1000L,
      stacks = PosInt.of(3)
    )

    // the end is noticed late, but the next shield starts when this one was supposed to end
    val actions = shield.endingAction(initialGameState, 1000L + shield.duration + 150L, None)
    assertEquals(actions.length, 1)
    actions.head match {
      case put: PutStackedBuff =>
        assertEquals(put.stacks, PosInt.of(2))
        assertEquals(put.bearerId, squareId)
        assertEquals(put.resourceIdentifier, Buff.squareBrace)
        assertEquals(put.appearanceTime, 1000L + shield.duration)
      case other => fail(s"Expected a PutStackedBuff, got $other")
    }
  }

  test("When the last stack ends, nothing replaces it") {
    val shield = BraceShield(genBuffId(), squareId, squareId, 1000L, stacks = PosInt.of(1))
    assertEquals(shield.endingAction(initialGameState, 1000L + shield.duration, None), Vector.empty)
  }

  test("A dead square does not get its shield back") {
    val shield = BraceShield(genBuffId(), squareId, squareId, 1000L, stacks = PosInt.of(4))
    assertEquals(shield.bearerDiedAction(initialGameState, 2000L), Vector.empty)
  }

  test("The stacks go down one at a time, each after the duration") {
    // Same thing as what the server does when a buff is over: remove it, and apply its ending.
    def expire(gameState: GameState): GameState = {
      val shield  = BraceShield.on(gameState, squareId).head
      val endTime = shield.appearanceTime + shield.duration
      gameState.applyActions(
        RemoveBuff(genActionId(), endTime, squareId, shield.buffId) +:
          shield.endingAction(gameState, endTime, None)
      )
    }

    val gameState = bracesAt(10, 20, 30)
    assertEquals(stacksAt(gameState), 3)

    val afterOne = expire(gameState)
    assertEquals(stacksAt(afterOne), 2)
    assertEquals(BraceShield.on(afterOne, squareId).length, 1)

    val afterTwo = expire(afterOne)
    assertEquals(stacksAt(afterTwo), 1)

    val afterThree = expire(afterTwo)
    assertEquals(BraceShield.on(afterThree, squareId), Vector.empty)
  }

}
