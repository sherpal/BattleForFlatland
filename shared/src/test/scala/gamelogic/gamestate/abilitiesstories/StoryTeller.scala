package gamelogic.gamestate.abilitiesstories

import gamelogic.utils.IdGeneratorContainer
import gamelogic.gamestate.gameactions.GameStart
import gamelogic.gamestate.gameactions.boss102.AddBossHound
import gamelogic.gamestate.gameactions.AddPlayerByClass
import models.bff.outofgame.PlayerClasses
import gamelogic.gamestate.gameactions.SpawnBoss
import gamelogic.gamestate.gameactions.UseAbility
import gamelogic.abilities.Ability
import gamelogic.gamestate.GameState
import gamelogic.entities.Entity
import gamelogic.entities.boss.Boss101

/**
  * Contains lots of facility methods for game action stories specs.
  */
trait StoryTeller extends munit.FunSuite {
  implicit val idGenerator: IdGeneratorContainer = IdGeneratorContainer.initialIdGeneratorContainer

  def gameStart(time: Long): GameStart = GameStart(idGenerator.gameActionIdGenerator(), time)
  def addHound(time: Long): AddBossHound =
    AddBossHound(idGenerator.gameActionIdGenerator(), time, idGenerator.entityIdGenerator(), 0)
  def addPlayer(time: Long): AddPlayerByClass =
    AddPlayerByClass(
      idGenerator.gameActionIdGenerator(),
      time,
      idGenerator.entityIdGenerator(),
      0,
      PlayerClasses.Triangle,
      0,
      "Hey"
    )
  def addHexagon(time: Long): AddPlayerByClass =
    AddPlayerByClass(
      idGenerator.gameActionIdGenerator(),
      time,
      idGenerator.entityIdGenerator(),
      0,
      PlayerClasses.Hexagon,
      0xFF0000,
      "TheHexagon"
    )
  def addBoss101(time: Long): SpawnBoss =
    SpawnBoss(idGenerator.gameActionIdGenerator(), time, idGenerator.entityIdGenerator(), Boss101.name)

  def nextUseId() = idGenerator.abilityUseIdGenerator()

  def useAbilityFromAbility(ability: Ability): UseAbility =
    UseAbility(idGenerator.gameActionIdGenerator(), ability.time, ability.casterId, ability.useId, ability)

  val initialGameState = GameState.empty
  val start            = gameStart(1)

  def assertEntitiesPresent(entityIds: Entity.Id*)(gameState: GameState): Unit =
    entityIds.map(gameState.entities.get).map(_.isDefined).foreach(assertEquals(_, true))

}
