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

final class TriangleStunSpecs extends munit.FunSuite {

  implicit val idGenerator: IdGeneratorContainer = IdGeneratorContainer.initialIdGeneratorContainer

  def gameStart(time: Long): GameStart = GameStart(idGenerator.gameActionIdGenerator(), time)
  def addHound(time: Long): AddBossHound = AddBossHound(idGenerator.gameActionIdGenerator(), time, idGenerator.entityIdGenerator(), 0)
  def addPlayer(time: Long): AddPlayerByClass = AddPlayerByClass(idGenerator.gameActionIdGenerator(), time, idGenerator.entityIdGenerator(), 0, PlayerClasses.Triangle, 0, "Hey")

  def nextUseId() = idGenerator.abilityUseIdGenerator()

  def useAbilityFromAbility(ability: Ability): UseAbility =
    UseAbility(idGenerator.gameActionIdGenerator(), ability.time, ability.casterId, ability.useId, ability)

  val initialGameState = GameState.empty
  val start = gameStart(1)

  def assertEntitiesPresent(entityIds: Entity.Id*)(gameState: GameState): Unit = 
    entityIds.map(gameState.entities.get).map(_.isDefined).foreach(assertEquals(_, true))

  test("Triangle uses stun then the target takes damage, the debuff should be removed") {
    val addingHound = addHound(2)
    val addingPlayer = addPlayer(2)

    val ability = Stun(nextUseId(), 3, addingPlayer.entityId, addingHound.entityId)
    val stunUse = useAbilityFromAbility(ability)

    val houndTakesDamage = EntityTakesDamage(idGenerator.gameActionIdGenerator(), 4, addingHound.entityId, 10, addingPlayer.entityId)

    val composer = ActionComposer.empty >>
      start >>
      addingHound >>
      addingPlayer >>>
      (gs => ability.createActions(gs)) >>
      stunUse >>>> { (gameState: GameState) =>
        (for {
          hound <- gameState.livingEntityAndMovingBodyById(addingHound.entityId)
          houndBuffs <- gameState.passiveBuffs.get(hound.id)
          houndStunDebuff <- houndBuffs.values.collectFirst { case s: TriangleStunDebuff => s }
        } yield houndStunDebuff) match {
          case Some(_) => // ok!
          case None => fail("The hound should have the stun debuff!")
        }
      } >>
      houndTakesDamage >>>> { (gameState: GameState) =>
        assertEntitiesPresent(addingPlayer.entityId, addingHound.entityId)(gameState)
        val hound = gameState.livingEntityAndMovingBodyById(addingHound.entityId).get
        assertEquals(hound.life, BossHound.houndMaxLife - houndTakesDamage.amount)
        assertNotEquals(
          for {
            houndBuffs <- gameState.passiveBuffs.get(hound.id)
            houndStunDebuff <- houndBuffs.values.collectFirst { case s: TriangleStunDebuff => s}
          } yield houndStunDebuff,
          Option.empty[TriangleStunDebuff]
        )
      }

  }

  test("Triangle uses stun on other hound, the first one should lose its debuff") {
    val addingFirstHound = addHound(2)
    val addingSecondHound = addHound(2)
    val addingPlayer = addPlayer(2)

    val ability = Stun(nextUseId(), 3, addingPlayer.entityId, addingFirstHound.entityId)
    val firstStunUse = useAbilityFromAbility(ability)

    val ability2 = Stun(nextUseId(), 4, addingPlayer.entityId, addingSecondHound.entityId)
    val secondStunUse = useAbilityFromAbility(ability2)

    val composer = ActionComposer.empty >>
      start >>
      addingFirstHound >> addingSecondHound >> addingPlayer >>>>
      assertEntitiesPresent(addingPlayer.entityId, addingFirstHound.entityId, addingSecondHound.entityId) >>>
      ((gs: GameState) => ability.createActions(gs)) >> firstStunUse >>>
      ((gs: GameState) => ability2.createActions(gs)) >> secondStunUse >>>>
      { (gs: GameState) =>
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
        assertEquals(for {
          hound2 <- gs.movingBodyEntityById(addingSecondHound.entityId)
          hound2Buffs <- gs.passiveBuffs.get(hound2.id)
          stunOnHound2 <- hound2Buffs.values.find(_.isInstanceOf[TriangleStunDebuff])
        } yield stunOnHound2.bearerId, Some(addingSecondHound.entityId))
      }

    composer(initialGameState)
  }

  test("Triangle uses stun and hound tries to move, it should not") {
    val addingHound = addHound(2)
    val addingPlayer = addPlayer(2)
    
    val useId = nextUseId()
    val ability = Stun(useId, 3, addingPlayer.entityId, addingHound.entityId)
    val playerStunHound = useAbilityFromAbility(ability)
    val houndMoves = MovingBodyMoves(idGenerator.gameActionIdGenerator(), 4, addingHound.entityId, 1, 0, 0, BossHound.fullSpeed, true)

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