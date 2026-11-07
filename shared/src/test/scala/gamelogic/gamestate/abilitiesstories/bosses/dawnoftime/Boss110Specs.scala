package gamelogic.gamestate.abilitiesstories.bosses.dawnoftime

import gamelogic.abilities.Ability
import gamelogic.abilities.boss.boss110.{ExplodeBombs, PlaceBombPods}
import gamelogic.gamestate.abilitiesstories.StoryTeller
import gamelogic.entities.Entity
import gamelogic.entities.WithPosition.Angle
import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import testutils.ActionComposer
import gamelogic.gamestate.gameactions.boss110.{AddBombPods, AddCreepingShadow}
import gamelogic.gamestate.gameactions.{
  AddPlayerByClass,
  EntityRadiusChange,
  EntityTakesDamage,
  RemoveEntity
}
import models.bff.outofgame.PlayerClasses
import gamelogic.entities.boss.boss110.CreepingShadow

final class Boss110Specs extends StoryTeller {

  val bossId = Entity.Id.zero

  def addCreepingShadowAction(time: Long): AddCreepingShadow = AddCreepingShadow(
    genActionId(),
    time,
    genEntityId(),
    bossId
  )

  def changeRadiusAction(time: Long, entityId: Entity.Id, radius: Double): EntityRadiusChange =
    EntityRadiusChange(genActionId(), time, entityId, radius)

  def getCreepingShadow(gs: GameState, entityId: Entity.Id): Option[CreepingShadow] =
    gs.entities.get(entityId).collect { case cs: CreepingShadow => cs }

  inline def assertCorrectRadius(gs: GameState, entityId: Entity.Id, radius: Double) = {
    val maybeCreepingShadow = getCreepingShadow(gs, entityId)
    assert(maybeCreepingShadow.nonEmpty)
    val creepingShadow = maybeCreepingShadow.get
    assertEquals(creepingShadow.radius, radius)
  }

  /** [[CreepingShadow]] tests
    */
  test("CreepingShadow change radius") {
    val addCreepingShadow = addCreepingShadowAction(0)
    val changeRadius      = changeRadiusAction(1, addCreepingShadow.entityId, 2)

    val composer = ActionComposer.empty >> start >> addCreepingShadow >>>> { (gs: GameState) =>
      assertCorrectRadius(gs, addCreepingShadow.entityId, 1)
    } >> changeRadius >>>> { (gs: GameState) =>
      assertCorrectRadius(gs, addCreepingShadow.entityId, 2)
    }

    composer(initialGameState)
  }

  test("Changing radius then moving works") {
    val addCreepingShadow = addCreepingShadowAction(0)
    val changeRadius      = changeRadiusAction(1, addCreepingShadow.entityId, 1)
    val moving =
      entityMoves(2, addCreepingShadow.entityId, 0, Angle.one, Angle.zero, 0, moving = true)

    val composer = ActionComposer.empty >> start >> addCreepingShadow >> changeRadius >>>> {
      (gs: GameState) =>
        assertCorrectRadius(gs, addCreepingShadow.entityId, changeRadius.radius)
    } >> moving >>>> { (gs: GameState) =>
      assertCorrectRadius(gs, addCreepingShadow.entityId, changeRadius.radius)
      val cs = getCreepingShadow(gs, addCreepingShadow.entityId).get // get is safe
      assertEquals(cs.pos, moving.position)
      assert(cs.moving)
    }

    composer(initialGameState)

    val gsWithShadow =
      addCreepingShadow.createGameStateTransformer(initialGameState)(initialGameState)
    val totalStateTransformer = changeRadius.createGameStateTransformer(gsWithShadow) ++ moving
      .createGameStateTransformer(gsWithShadow)

    val gs             = totalStateTransformer(gsWithShadow)
    val creepingShadow = getCreepingShadow(gs, addCreepingShadow.entityId).get

    assert(creepingShadow.moving)
    assertCorrectRadius(gs, addCreepingShadow.entityId, changeRadius.radius)
    assertEquals(creepingShadow.pos, moving.position)

  }

  /** Bombs tests
    */
  val bombPositions: Vector[Complex] = Vector(Complex(-300, 0), Complex(-200, 0))

  def addPlayerAt(time: Long, position: Complex): AddPlayerByClass =
    AddPlayerByClass(
      genActionId(),
      time,
      genEntityId(),
      position,
      PlayerClasses.Pentagon,
      0,
      "Bomb Squad"
    )

  /** The game state with a player at each of the given positions, and the bombs down. */
  def withBombsAndPlayersAt(playerPositions: Complex*): GameState =
    initialGameState.applyActions(
      Vector(start) ++ playerPositions.map(addPlayerAt(2, _)) :+ AddBombPods(
        genActionId(),
        3,
        bombPositions.map(genEntityId() -> _).toList,
        bossId
      )
    )

  def explosionDamage(gs: GameState): Vector[EntityTakesDamage] =
    ExplodeBombs(Ability.UseId.zero, 10, bossId).createActions(gs).collect {
      case damage: EntityTakesDamage => damage
    }

  test("Bombs covered by exactly one player each do not explode") {
    val gs      = withBombsAndPlayersAt(bombPositions*)
    val actions = ExplodeBombs(Ability.UseId.zero, 10, bossId).createActions(gs)
    assertEquals(explosionDamage(gs), Vector.empty)
    assertEquals(actions.collect { case remove: RemoveEntity => remove }.length, 2)
  }

  test("An uncovered bomb hurts everybody") {
    val gs = withBombsAndPlayersAt(bombPositions.head, Complex(0, 200))
    assertEquals(explosionDamage(gs).map(_.entityId).toSet, gs.players.keySet)
    assert(explosionDamage(gs).forall(_.amount == ExplodeBombs.damage))
  }

  test("A bomb covered by two players hurts everybody") {
    val gs = withBombsAndPlayersAt(bombPositions.head, bombPositions.head + 3, bombPositions.last)
    assertEquals(explosionDamage(gs).map(_.entityId).toSet, gs.players.keySet)
  }

  test("Nobody can cover two bombs at once") {
    (1 to 100).foreach { _ =>
      val positions = PlaceBombPods.randomPositionsInSquare(
        PlaceBombPods.bombsAreaCenter,
        PlaceBombPods.bombsAreaRadius,
        PlaceBombPods.numberOfBombs
      )
      assertEquals(positions.length, PlaceBombPods.numberOfBombs)
      for {
        (z, i) <- positions.zipWithIndex
        (w, j) <- positions.zipWithIndex
        if i < j
      } assert(z.distanceTo(w) > PlaceBombPods.minimumDistanceBetweenBombs)
    }
  }

}
