package gamelogic.gamestate.abilitiesstories.bosses.dawnoftime

import gamelogic.abilities.Ability
import gamelogic.abilities.boss.boss104.DeathMarkTriangleAttack
import gamelogic.buffs.boss.boss104.BigGuyCurse
import gamelogic.entities.Entity
import gamelogic.entities.boss.boss104.{BigGuyDeathMark, DeathMarkTriangle}
import gamelogic.gamestate.abilitiesstories.StoryTeller
import gamelogic.gamestate.gameactions.boss104.{AddBigGuy, DeathMarkTriangleNextStage}
import gamelogic.gamestate.gameactions.{
  AddPlayerByClass,
  EntityTakesDamage,
  RemoveEntity
}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import models.bff.outofgame.PlayerClasses
import testutils.ActionComposer

/** Story of Boss104's two-stage death mark triangle attack.
  *
  * The three big guys die at `-150`, `150` and `150i`, which makes a triangle whose centre of
  * gravity is `50i`.
  */
final class Boss104Specs extends StoryTeller {

  val bossId = Entity.Id.zero

  val deathPositions: Vector[Complex] = Vector(-150, 150, Complex(0, 150))

  /** Well inside the triangle. */
  val insidePosition: Complex = Complex(0, 50)

  /** Well outside of it. */
  val outsidePosition: Complex = Complex(1000, 1000)

  def addBigGuyAt(time: Long, position: Complex): AddBigGuy =
    AddBigGuy(genActionId(), time, genEntityId(), genBuffId(), genBuffId(), position)

  def addPlayerAt(time: Long, position: Complex, name: String): AddPlayerByClass =
    AddPlayerByClass(genActionId(), time, genEntityId(), position, PlayerClasses.Square, 0, name)

  /** Mimics what [[gamelogic.gamestate.serveractions.ManageDeadAIs]] does when a big guy dies: run
    * the ending actions of its buffs, then remove it.
    */
  def killBigGuy(bigGuyId: Entity.Id, time: Long)(gameState: GameState): Vector[GameAction] = {
    val curseActions = gameState
      .allBuffsOfEntity(bigGuyId)
      .collect { case curse: BigGuyCurse => curse }
      .flatMap(_.bearerDiedAction(gameState, time))
      .toVector

    curseActions :+ RemoveEntity(genActionId(), time, bigGuyId)
  }

  def theTriangle(gameState: GameState): Option[DeathMarkTriangle] =
    gameState.allTEntities[DeathMarkTriangle].values.headOption

  test("The triangle only appears once the third big guy has died") {
    val bigGuys = deathPositions.zipWithIndex.map((position, i) => addBigGuyAt(i + 1L, position))

    val composer = ActionComposer.empty >> start >>
      bigGuys(0) >> bigGuys(1) >> bigGuys(2) >>>> { (gs: GameState) =>
        assertEquals(gs.allTEntities[BigGuyDeathMark].size, 0)
        assertEquals(theTriangle(gs), None)
      } >>> killBigGuy(bigGuys(0).entityId, 10) >>>> { (gs: GameState) =>
        assertEquals(gs.allTEntities[BigGuyDeathMark].size, 1)
        assertEquals(theTriangle(gs), None)
      } >>> killBigGuy(bigGuys(1).entityId, 20) >>>> { (gs: GameState) =>
        assertEquals(gs.allTEntities[BigGuyDeathMark].size, 2)
        assertEquals(theTriangle(gs), None)
      } >>> killBigGuy(bigGuys(2).entityId, 30) >>>> { (gs: GameState) =>
        assertEquals(gs.allTEntities[BigGuyDeathMark].size, 3)

        val triangle = theTriangle(gs).get
        assertEquals(triangle.vertices.toSet, deathPositions.toSet)
        assertEquals(triangle.stage, DeathMarkTriangle.firstStage)
        assert(triangle.insideIsDeadly)

        // the geometry actually works: whatever order the marks came in, the polygon is properly
        // oriented and knows its inside from its outside
        assertEquals(triangle.damageAt(insidePosition), DeathMarkTriangle.deadlyDamage)
        assertEquals(triangle.damageAt(outsidePosition), DeathMarkTriangle.safeDamage)
      }

    composer(initialGameState)
  }

  test("The attack hits the inside, then the outside, then cleans up after itself") {
    val bigGuys = deathPositions.zipWithIndex.map((position, i) => addBigGuyAt(i + 1L, position))

    val playerIn  = addPlayerAt(4, insidePosition, "Inside")
    val playerOut = addPlayerAt(5, outsidePosition, "Outside")

    def attackAt(time: Long)(gameState: GameState): Vector[GameAction] =
      DeathMarkTriangleAttack(Ability.UseId.zero, time, bossId).createActions(gameState)

    def damageTo(entityId: Entity.Id, actions: Vector[GameAction]): Double =
      actions.collectFirst { case damage: EntityTakesDamage if damage.entityId == entityId =>
        damage.amount
      }.get

    val composer = ActionComposer.empty >> start >> playerIn >> playerOut >>
      bigGuys(0) >> bigGuys(1) >> bigGuys(2) >>>
      killBigGuy(bigGuys(0).entityId, 10) >>>
      killBigGuy(bigGuys(1).entityId, 20) >>>> { (gs: GameState) =>
        // with no triangle around, the boss simply may not cast it
        assert(
          DeathMarkTriangleAttack(Ability.UseId.zero, 25, bossId).canBeCast(gs, 25).isDefined
        )
      } >>> killBigGuy(bigGuys(2).entityId, 30) >>>> { (gs: GameState) =>
        assert(DeathMarkTriangleAttack(Ability.UseId.zero, 35, bossId).canBeCast(gs, 35).isEmpty)

        // first stage: being inside is what kills
        val actions = attackAt(40)(gs)
        assertEquals(damageTo(playerIn.entityId, actions), DeathMarkTriangle.deadlyDamage)
        assertEquals(damageTo(playerOut.entityId, actions), DeathMarkTriangle.safeDamage)
        assertEquals(actions.count(_.isInstanceOf[DeathMarkTriangleNextStage]), 1)
        assertEquals(actions.count(_.isInstanceOf[RemoveEntity]), 0)
      } >>> attackAt(40) >>>> { (gs: GameState) =>
        val triangle = theTriangle(gs).get
        assertEquals(triangle.stage, DeathMarkTriangle.secondStage)
        assert(!triangle.insideIsDeadly)

        // second stage: the very same ability now does the exact opposite
        val actions = attackAt(50)(gs)
        assertEquals(damageTo(playerIn.entityId, actions), DeathMarkTriangle.safeDamage)
        assertEquals(damageTo(playerOut.entityId, actions), DeathMarkTriangle.deadlyDamage)
        assertEquals(actions.count(_.isInstanceOf[DeathMarkTriangleNextStage]), 0)
        // the triangle and the three marks it was built on
        assertEquals(actions.count(_.isInstanceOf[RemoveEntity]), 4)
      } >>> attackAt(50) >>>> { (gs: GameState) =>
        // the fight goes back to normal, ready for the next three big guys
        assertEquals(theTriangle(gs), None)
        assertEquals(gs.allTEntities[BigGuyDeathMark].size, 0)
        assert(DeathMarkTriangleAttack(Ability.UseId.zero, 60, bossId).canBeCast(gs, 60).isDefined)
      }

    composer(initialGameState)
  }

}
