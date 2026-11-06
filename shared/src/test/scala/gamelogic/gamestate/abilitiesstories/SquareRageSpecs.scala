package gamelogic.gamestate.abilitiesstories

import gamelogic.abilities.square.Brace
import gamelogic.buffs.resourcebuffs.{RageFiller, RageTrickle}
import gamelogic.entities.Resource.Rage
import gamelogic.entities.Entity
import gamelogic.entities.classes.Square
import gamelogic.gamestate.GameState
import gamelogic.gamestate.gameactions.{AddPlayerByClass, EntityResourceChanges, EntityTakesDamage}
import models.bff.outofgame.PlayerClasses
import testutils.ActionComposer

final class SquareRageSpecs extends StoryTeller {

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

  private def rageOf(gameState: GameState): Double =
    gameState.withAbilityEntitiesById(squareId).get.resourceAmount.amount

  /** What the server does when it creates a square: add it, then give it its starting buffs. */
  private val squareWithItsBuffs =
    ActionComposer.empty >>
      start >>
      addingSquare >>>
      (_ => Square.startingActions(2, squareId))

  /** A square without any rage, which has braced `braces` times. */
  private def emptySquareWithBraces(braces: Int): GameState =
    (1 to braces).foldLeft(
      squareWithItsBuffs >>
        EntityResourceChanges(genActionId(), 3, squareId, -1000.0, Rage)
    ) { (composer, i) =>
      val ability = Brace(nextUseId(), 10L * i, squareId)
      composer >>> (gs => ability.createActions(gs))
    }(initialGameState)

  private def hit(amount: Double) =
    EntityTakesDamage(genActionId(), 100, squareId, amount, genEntityId())

  test("The square starts with all its rage") {
    assertEqualsDouble(rageOf(squareWithItsBuffs(initialGameState)), 100.0, 1e-9)
  }

  test("The rage gained is proportional to the damage taken") {
    val gameState = emptySquareWithBraces(0)
    assertEqualsDouble(rageOf(gameState), 0.0, 1e-9)
    assertEqualsDouble(rageOf(hit(5)(gameState)), 5 * RageFiller.ragePerDamage, 1e-9)
    assertEqualsDouble(rageOf(hit(40)(gameState)), 40 * RageFiller.ragePerDamage, 1e-9)
  }

  test("The rage gained does not depend on how much the shield reduces the damage") {
    val withoutShield = hit(40)(emptySquareWithBraces(0))
    val withShield    = hit(40)(emptySquareWithBraces(3))
    assertEqualsDouble(rageOf(withShield), rageOf(withoutShield), 1e-9)
  }

  test("The rage is capped by what the square can hold") {
    val gameState = hit(1000)(emptySquareWithBraces(0))
    assertEqualsDouble(rageOf(gameState), 100.0, 1e-9)
  }

  test("The trickle gives a bit of rage at each tick") {
    val gameState = emptySquareWithBraces(0)
    val trickle = RageTrickle(
      gameState
        .allBuffsOfEntity(squareId)
        .collectFirst { case t: RageTrickle =>
          t.buffId
        }
        .get,
      squareId,
      0L,
      0L
    )
    val afterTick = gameState.applyActions(trickle.tickEffect(gameState, 1000L))
    assertEqualsDouble(rageOf(afterTick), RageTrickle.rageRefillPerSecond, 1e-9)
  }

  test("The square has the trickle from the start") {
    val gameState = squareWithItsBuffs(initialGameState)
    assert(gameState.allBuffsOfEntity(squareId).exists(_.isInstanceOf[RageTrickle]))
  }

}
