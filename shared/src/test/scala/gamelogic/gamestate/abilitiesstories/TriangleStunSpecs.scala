package gamelogic.gamestate.abilitiesstories

import gamelogic.gamestate.gameactions.boss102.AddBossHound
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
import shapeless.ops.fin
import gamelogic.physics.Complex
import testutils.ActionComposer
import gamelogic.abilities.Ability
import gamelogic.buffs.abilities.classes.TriangleStunDebuff
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.gameactions.EntityStartsCasting
import gamelogic.abilities.hexagon.FlashHeal
import gamelogic.gamestate.gameactions.SpawnBoss
import gamelogic.entities.boss.BossFactory
import gamelogic.entities.boss.Boss101

final class TriangleStunSpecs extends StoryTeller {

  test("Stun on a boss should be illegal") {
    val addingBoss   = addBoss101(2)
    val addingPlayer = addPlayer(2)

    val stun = Stun(nextUseId(), 4, addingPlayer.entityId, addingBoss.entityId)

    val composer = ActionComposer.empty >> start >> addingBoss >> addingPlayer >>>> { (gs: GameState) =>
      assertEquals(stun.isInRangeAndInSight(gs, 3), None)
      assertEquals(stun.canBeCast(gs, 3), Some("Target can't be stunned"))
    }

    composer(initialGameState)
  }

  test("Triangle uses stun on a casting target, the cast should be interrupted") {
    val addingPlayer = addPlayer(2)
    val addingFoe    = addHexagon(2)

    val foeStartsCasting = EntityStartsCasting(
      idGenerator.gameActionIdGenerator(),
      3,
      3L,
      FlashHeal(idGenerator.abilityUseIdGenerator(), 3, addingFoe.entityId, addingFoe.entityId)
    )

    val ability = Stun(nextUseId(), 4, addingPlayer.entityId, addingFoe.entityId)
    val stunUse = useAbilityFromAbility(ability)

    val composer = ActionComposer.empty >>
      start >>
      addingFoe >> addingPlayer >>
      foeStartsCasting >>>> { (gameState: GameState) =>
      assert(gameState.entityIsCasting(addingFoe.entityId))
    } >>> (gs => ability.createActions(gs)) >> stunUse >>>> { (gameState: GameState) =>
      assert(
        !gameState.entityIsCasting(addingFoe.entityId),
        s"Foe is still casting, casting info were ${gameState.castingEntityInfo}."
      )
    }

    composer(initialGameState)
  }

  test("Triangle uses stun then the target takes damage, the debuff should be removed") {
    val addingHound  = addHound(2)
    val addingPlayer = addPlayer(2)

    val ability = Stun(nextUseId(), 3, addingPlayer.entityId, addingHound.entityId)
    val stunUse = useAbilityFromAbility(ability)

    val houndTakesDamage =
      EntityTakesDamage(idGenerator.gameActionIdGenerator(), 4, addingHound.entityId, 10, addingPlayer.entityId)

    val composer = ActionComposer.empty >>
      start >>
      addingHound >>
      addingPlayer >>>
      (gs => ability.createActions(gs)) >>
      stunUse >>>> { (gameState: GameState) =>
      (for {
        hound           <- gameState.livingEntityAndMovingBodyById(addingHound.entityId)
        houndBuffs      <- gameState.passiveBuffs.get(hound.id)
        houndStunDebuff <- houndBuffs.values.collectFirst { case s: TriangleStunDebuff => s }
      } yield houndStunDebuff) match {
        case Some(_) => // ok!
        case None    => fail("The hound should have the stun debuff!")
      }
    } >>
      houndTakesDamage >>>> { (gameState: GameState) =>
      assertEntitiesPresent(addingPlayer.entityId, addingHound.entityId)(gameState)
      val hound = gameState.livingEntityAndMovingBodyById(addingHound.entityId).get
      assertEquals(hound.life, BossHound.houndMaxLife - houndTakesDamage.amount)
      assertNotEquals(
        for {
          houndBuffs      <- gameState.passiveBuffs.get(hound.id)
          houndStunDebuff <- houndBuffs.values.collectFirst { case s: TriangleStunDebuff => s }
        } yield houndStunDebuff,
        Option.empty[TriangleStunDebuff]
      )
    }

  }

  test("Triangle uses stun on other hound, the first one should lose its debuff") {
    val addingFirstHound  = addHound(2)
    val addingSecondHound = addHound(2)
    val addingPlayer      = addPlayer(2)

    val ability      = Stun(nextUseId(), 3, addingPlayer.entityId, addingFirstHound.entityId)
    val firstStunUse = useAbilityFromAbility(ability)

    val ability2      = Stun(nextUseId(), 4, addingPlayer.entityId, addingSecondHound.entityId)
    val secondStunUse = useAbilityFromAbility(ability2)

    val composer = ActionComposer.empty >>
      start >>
      addingFirstHound >> addingSecondHound >> addingPlayer >>>>
      assertEntitiesPresent(addingPlayer.entityId, addingFirstHound.entityId, addingSecondHound.entityId) >>>
      ((gs: GameState) => ability.createActions(gs)) >> firstStunUse >>>
      ((gs: GameState) => ability2.createActions(gs)) >> secondStunUse >>>> { (gs: GameState) =>
      gs.movingBodyEntityById(addingFirstHound.entityId) match {
        case None => fail("First hound was not there at the end!")
        case Some(hound) =>
          gs.passiveBuffs.get(hound.id) match {
            case Some(value) =>
              assertEquals(value.values.collectFirst {
                case stun: TriangleStunDebuff => stun
              }, None)
            case None => // ok!
          }
      }
      assertEquals(
        for {
          hound2       <- gs.movingBodyEntityById(addingSecondHound.entityId)
          hound2Buffs  <- gs.passiveBuffs.get(hound2.id)
          stunOnHound2 <- hound2Buffs.values.find(_.isInstanceOf[TriangleStunDebuff])
        } yield stunOnHound2.bearerId,
        Some(addingSecondHound.entityId)
      )
    }

    composer(initialGameState)
  }

  test("Triangle uses stun and hound tries to move, it should not") {
    val addingHound  = addHound(2)
    val addingPlayer = addPlayer(2)

    val useId           = nextUseId()
    val ability         = Stun(useId, 3, addingPlayer.entityId, addingHound.entityId)
    val playerStunHound = useAbilityFromAbility(ability)
    val houndMoves =
      MovingBodyMoves(idGenerator.gameActionIdGenerator(), 4, addingHound.entityId, 1, 0, 0, BossHound.fullSpeed, true)

    val composer = ActionComposer.empty >>
      start >>
      addingHound >>
      addingPlayer >>>>
      ((gs: GameState) => assert(gs.movingBodyEntityById(addingPlayer.entityId).isDefined)) >>
      playerStunHound >>>
      ((gs: GameState) => ability.createActions(gs)) >>
      houndMoves

    val finalGameState = composer(initialGameState)

    finalGameState.movingBodyEntityById(addingHound.entityId) match {
      case None => fail("Hound was not there at end of actions")
      case Some(hound) =>
        assertEquals(hound.pos, 0: Complex)
    }
  }

}
