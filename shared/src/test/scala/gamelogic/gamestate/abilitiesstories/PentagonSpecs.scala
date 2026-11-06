package gamelogic.gamestate.abilitiesstories

import gamelogic.abilities.pentagon.{CreatePentagonBullet, Fracture, Pentagram}
import gamelogic.buffs.abilities.classes.{Facets, Fractured}
import gamelogic.entities.Entity
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.boss.boss102.BossHound
import gamelogic.entities.classes.{Constants, Pentagon}
import gamelogic.entities.classes.pentagon.SteadyAim
import gamelogic.entities.movingstuff.PentagonBullet
import gamelogic.gamestate.gameactions.{
  AddPlayerByClass,
  EntityTakesDamage,
  NewPentagonBullet,
  PutStackedBuff
}
import gamelogic.gamestate.serveractions.ManagePentagonBullets
import gamelogic.gamestate.{GameState, GreedyActionGatherer}
import gamelogic.physics.{Complex, PosInt}
import models.bff.outofgame.PlayerClasses
import testutils.ActionComposer

final class PentagonSpecs extends StoryTeller {

  private val addingPentagon = AddPlayerByClass(
    genActionId(),
    2,
    genEntityId(),
    0,
    PlayerClasses.Pentagon,
    0,
    "ThePentagon"
  )
  private val pentagonId: Entity.Id = addingPentagon.entityId

  private val addingHound        = addHound(2)
  private val houndId: Entity.Id = addingHound.entityId

  private val addingHexagon        = addHexagon(2)
  private val hexagonId: Entity.Id = addingHexagon.entityId

  /** A pentagon still since time 2, next to a hound and a hexagon. */
  private val situation: GameState =
    (ActionComposer.empty >> start >> addingPentagon >> addingHound >> addingHexagon)(
      initialGameState
    )

  private def withFacets(gameState: GameState, facets: Int): GameState =
    gameState.applyActions(
      Facets.setStacks(gameState, pentagonId, gameState.time, PosInt.maybe(facets))
    )

  private def pentagon(gameState: GameState): Pentagon =
    gameState.players(pentagonId).asInstanceOf[Pentagon]

  private def houndLife(gameState: GameState): Double =
    gameState.livingEntityById(houndId).get.life

  private def facetsOf(gameState: GameState): Int =
    Facets.stacksOn(gameState, pentagonId).fold(0)(_.toInt)

  /** Bullets put on the tip of the hound, which the server then sees hitting it at `time`. */
  private def bulletsHit(gameState: GameState, time: Long, fractures: Boolean*): GameState = {
    val withBullets = gameState.applyActions(fractures.toVector.map { fracturing =>
      NewPentagonBullet(
        genActionId(),
        time,
        genEntityId(),
        Complex(Constants.playerRadius, 0),
        PentagonBullet.defaultSpeed,
        Angle.zero,
        PentagonBullet.defaultRange,
        CreatePentagonBullet.damage,
        pentagonId,
        Entity.teams.playerTeam,
        0,
        fracturing
      )
    })
    ManagePentagonBullets()(GreedyActionGatherer(withBullets), () => time)._1.currentGameState
  }

  test("Steady Aim gains a stack every 2 seconds without moving, up to the maximum") {
    val still = pentagon(situation)
    assertEquals(SteadyAim.stacks(still, 2 + SteadyAim.stackEvery - 1), 0)
    assertEquals(SteadyAim.stacks(still, 2 + 2 * SteadyAim.stackEvery), 2)
    assertEquals(SteadyAim.stacks(still, 2 + 100 * SteadyAim.stackEvery), SteadyAim.maxStacks)
  }

  test("Moving loses Steady Aim, which starts again once stopped") {
    val moved = situation.applyActions(
      Vector(
        entityMoves(20000, pentagonId, Complex.zero, Angle.zero, Angle.zero, 100, moving = true),
        entityMoves(21000, pentagonId, Complex(100, 0), Angle.zero, Angle.zero, 100, false)
      )
    )
    assertEquals(SteadyAim.stacks(pentagon(moved), 20500), 0)
    assertEquals(SteadyAim.stacks(pentagon(moved), 21000 + SteadyAim.stackEvery), 1)
  }

  test("Turning without moving keeps Steady Aim") {
    val turned = situation.applyActions(
      Vector(entityMoves(20000, pentagonId, Complex.zero, Angle.zero, Angle.Pi, 0, false))
    )
    assertEquals(SteadyAim.stacks(pentagon(turned), 20000), SteadyAim.maxStacks)
  }

  test("Steady Aim increases the damage of the bullets when they are fired") {
    val ability = CreatePentagonBullet(
      nextUseId(),
      2 + 3 * SteadyAim.stackEvery,
      pentagonId,
      Complex.zero,
      CreatePentagonBullet.damage,
      Angle.zero,
      0
    )
    val damages = ability.createActions(situation).collect { case bullet: NewPentagonBullet =>
      bullet.damage
    }
    assertEquals(damages.length, 1)
    assertEqualsDouble(
      damages.head,
      CreatePentagonBullet.damage * (1 + 3 * SteadyAim.damageIncreasePerStack),
      1e-9
    )
  }

  test("A bullet hitting deals its damage and lights a facet") {
    val after = bulletsHit(situation, 100, false)
    assertEqualsDouble(houndLife(after), BossHound.houndMaxLife - CreatePentagonBullet.damage, 1e-9)
    assertEquals(facetsOf(after), 1)
    assertEquals(after.pentagonBullets, Map.empty)
  }

  test("Bullets hitting together light a facet each") {
    assertEquals(facetsOf(bulletsHit(withFacets(situation, 1), 100, false, false)), 3)
  }

  test("Bullets hitting while all the facets are lit are wasted") {
    val after = bulletsHit(withFacets(situation, 4), 100, false, false)
    assertEquals(Facets.on(after, pentagonId).map(_.stacks), Vector(Facets.maxStacks))
  }

  test("A facet goes out after a while") {
    val lit     = withFacets(situation, 3)
    val facets  = Facets.on(lit, pentagonId).head
    val endings = facets.endingAction(lit, facets.appearanceTime + Facets.duration, None)
    assertEquals(
      endings.collect { case put: PutStackedBuff => (put.time, put.stacks.toInt) },
      Vector((facets.appearanceTime + Facets.duration, 2))
    )
  }

  test("Pentagram needs all the facets") {
    val pentagram = Pentagram(nextUseId(), 100, pentagonId, houndId)
    assertEquals(
      pentagram.canBeCast(withFacets(situation, 4), 100),
      Some("All your facets must be lit")
    )
    assertEquals(pentagram.canBeCast(withFacets(situation, 5), 100), None)
  }

  test("Pentagram can only target enemies") {
    assertEquals(
      Pentagram(nextUseId(), 100, pentagonId, hexagonId).canBeCast(withFacets(situation, 5), 100),
      Some("Not an enemy")
    )
  }

  test("Pentagram hits the target and spends all the facets") {
    val before = withFacets(situation, 5)
    // Still for less than a stack of Steady Aim, so that its damage is the base one
    val after =
      before.applyActions(Pentagram(nextUseId(), 100, pentagonId, houndId).createActions(before))
    assertEqualsDouble(houndLife(after), BossHound.houndMaxLife - Pentagram.damage, 1e-9)
    assertEquals(Facets.on(after, pentagonId), Vector.empty)
  }

  test("A fracturing bullet fractures the enemy, and lights no facet") {
    val after = bulletsHit(situation, 100, true)
    assertEquals(facetsOf(after), 0)
    val fractured = Fractured.by(after, houndId, pentagonId)
    assertEquals(fractured.map(_.duration), Some(Fractured.duration))
  }

  test("A fractured enemy takes more damage from its pentagon only") {
    val fractured  = bulletsHit(situation, 100, true)
    val lifeBefore = houndLife(fractured)
    val hitByPentagon =
      EntityTakesDamage(genActionId(), 200, houndId, 100, pentagonId)(fractured)
    val hitByHexagon =
      EntityTakesDamage(genActionId(), 200, houndId, 100, hexagonId)(fractured)
    assertEqualsDouble(
      lifeBefore - houndLife(hitByPentagon),
      100 * (1 + Fractured.damageIncrease),
      1e-9
    )
    assertEqualsDouble(lifeBefore - houndLife(hitByHexagon), 100, 1e-9)
  }

  test("Fracturing again carries over what was left, up to a maximum") {
    val first = bulletsHit(situation, 100, true)

    val soon = bulletsHit(first, 100 + Fractured.duration - 1000, true)
    assertEquals(
      Fractured.by(soon, houndId, pentagonId).map(_.duration),
      Some(Fractured.duration + 1000)
    )

    val early = bulletsHit(first, 1100, true)
    assertEquals(
      Fractured.by(early, houndId, pentagonId).map(_.duration),
      Some(Fractured.duration + Fractured.maxCarriedOver)
    )
    assertEquals(early.allBuffsOfEntity(houndId).count(_.isInstanceOf[Fractured]), 1)
  }

  test("Fracture fires a fracturing bullet") {
    val bullets =
      Fracture(nextUseId(), 100, pentagonId, Complex.zero, Fracture.damage, Angle.zero, 0)
        .createActions(situation)
        .collect { case bullet: NewPentagonBullet => bullet }
    assertEquals(bullets.map(_.fractures), Vector(true))
  }

}
