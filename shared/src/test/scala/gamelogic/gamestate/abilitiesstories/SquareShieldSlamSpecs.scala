package gamelogic.gamestate.abilitiesstories

import gamelogic.abilities.square.{Brace, ShieldSlam}
import gamelogic.buffs.abilities.classes.BraceShield
import gamelogic.entities.Entity
import gamelogic.entities.boss.boss102.BossHound
import gamelogic.gamestate.GameState
import gamelogic.gamestate.gameactions.AddPlayerByClass
import models.bff.outofgame.PlayerClasses
import testutils.ActionComposer

final class SquareShieldSlamSpecs extends StoryTeller {

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

  private val addingHound        = addHound(2)
  private val houndId: Entity.Id = addingHound.entityId

  private val addingHexagon        = addHexagon(2)
  private val hexagonId: Entity.Id = addingHexagon.entityId

  /** A square next to a hound and a hexagon, which has braced `braces` times (at times 10, 20...).
    */
  private def situation(braces: Int): GameState =
    (1 to braces).foldLeft(
      ActionComposer.empty >> start >> addingSquare >> addingHound >> addingHexagon
    ) { (composer, i) =>
      val ability = Brace(nextUseId(), 10L * i, squareId)
      composer >>> (gs => ability.createActions(gs))
    }(initialGameState)

  private def slam(time: Long, targetId: Entity.Id = houndId) =
    ShieldSlam(nextUseId(), time, squareId, targetId)

  private def slamUsed(gameState: GameState, ability: ShieldSlam): GameState =
    gameState.applyActions(ability.createActions(gameState))

  private def stacksOf(gameState: GameState): Int =
    BraceShield.stacksOn(gameState, squareId).fold(0)(_.toInt)

  test("Shield Slam needs a stack of the shield") {
    assertEquals(slam(100).canBeCast(situation(0), 100), Some("You need a stack of your shield"))
    assertEquals(slam(100).canBeCast(situation(1), 100), None)
  }

  test("Shield Slam can only target enemies") {
    assertEquals(slam(100, hexagonId).canBeCast(situation(2), 100), Some("Not an enemy"))
  }

  test("Shield Slam hits the target") {
    val gameState = slamUsed(situation(2), slam(100))
    val hound     = gameState.livingEntityById(houndId).get
    assertEqualsDouble(hound.life, BossHound.houndMaxLife - ShieldSlam.damage, 1e-9)
  }

  test("Shield Slam generates extra threat") {
    val gameState = slamUsed(situation(2), slam(100))
    val threat    = gameState.withThreatEntityById(houndId).get.damageThreats.get(squareId)
    assert(
      threat.exists(_.value >= ShieldSlam.extraThreat.value),
      s"Threat was $threat, but should be at least ${ShieldSlam.extraThreat}"
    )
  }

  test("Shield Slam spends one stack and resets the time of the others") {
    val before = situation(3)
    assertEquals(stacksOf(before), 3)

    val after = slamUsed(before, slam(100))
    assertEquals(stacksOf(after), 2)
    assertEquals(BraceShield.on(after, squareId).length, 1)
    assertEquals(BraceShield.on(after, squareId).head.appearanceTime, 100L)
  }

  test("Shield Slam with the last stack leaves no shield") {
    val after = slamUsed(situation(1), slam(100))
    assertEquals(BraceShield.on(after, squareId), Vector.empty)
  }

}
