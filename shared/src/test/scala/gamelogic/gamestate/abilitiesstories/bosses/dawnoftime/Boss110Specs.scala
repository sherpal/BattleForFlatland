package gamelogic.gamestate.abilitiesstories.bosses.dawnoftime

import gamelogic.gamestate.abilitiesstories.StoryTeller
import gamelogic.utils.IdGeneratorContainer
import gamelogic.gamestate.GameAction
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.AddPlayerByClass
import models.bff.outofgame.PlayerClasses
import gamelogic.gamestate.GameState
import gamelogic.gamestate.gameactions.GameStart
import gamelogic.gamestate.gameactions.UseAbility
import gamelogic.abilities.triangle.Stun
import gamelogic.gamestate.gameactions.MovingBodyMoves
import gamelogic.entities.boss.boss102.BossHound
import gamelogic.physics.Complex
import testutils.ActionComposer
import gamelogic.abilities.Ability
import gamelogic.buffs.abilities.classes.TriangleStunDebuff
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.gameactions.EntityStartsCasting
import gamelogic.abilities.hexagon.FlashHeal
import gamelogic.gamestate.gameactions.SpawnBoss
import gamelogic.entities.boss.BossFactory
import gamelogic.gamestate.gameactions.boss110.AddCreepingShadow
import gamelogic.gamestate.gameactions.EntityRadiusChange
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
    val moving            = entityMoves(2, addCreepingShadow.entityId, 0, 1, 0, 0, moving = true)

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

}
