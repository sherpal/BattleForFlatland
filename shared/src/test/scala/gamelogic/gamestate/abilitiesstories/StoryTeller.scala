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
import gamelogic.gamestate.gameactions.MovingBodyMoves
import gamelogic.entities.WithPosition.Angle
import gamelogic.physics.Complex
import gamelogic.utils.IdsProducer

/** Contains lots of facility methods for game action stories specs.
  */
trait StoryTeller extends munit.FunSuite with IdsProducer {
  given IdGeneratorContainer = IdGeneratorContainer.initialIdGeneratorContainer

  def gameStart(time: Long): GameStart = GameStart(genActionId(), time)
  def addHound(time: Long): AddBossHound =
    AddBossHound(genActionId(), time, genEntityId(), 0)
  def addPlayer(time: Long): AddPlayerByClass =
    AddPlayerByClass(
      genActionId(),
      time,
      genEntityId(),
      0,
      PlayerClasses.Triangle,
      0,
      "Hey"
    )
  def addHexagon(time: Long): AddPlayerByClass =
    AddPlayerByClass(
      genActionId(),
      time,
      genEntityId(),
      0,
      PlayerClasses.Hexagon,
      0xff0000,
      "TheHexagon"
    )
  def addBoss101(time: Long): SpawnBoss =
    SpawnBoss(
      genActionId(),
      time,
      genEntityId(),
      Boss101.name
    )

  def entityMoves(
      time: Long,
      entityId: Entity.Id,
      position: Complex,
      direction: Angle,
      rotation: Angle,
      speed: Double,
      moving: Boolean
  ): MovingBodyMoves =
    MovingBodyMoves(
      genActionId(),
      time,
      entityId,
      position,
      direction,
      rotation,
      speed,
      moving
    )

  def nextUseId() = genAbilityUseId()

  def useAbilityFromAbility(ability: Ability): UseAbility =
    UseAbility(
      genActionId(),
      ability.time,
      ability.casterId,
      ability.useId,
      ability
    )

  val initialGameState = GameState.empty
  val start            = gameStart(1)

  def assertEntitiesPresent(entityIds: Entity.Id*)(gameState: GameState): Unit =
    entityIds.map(gameState.entities.get).map(_.isDefined).foreach(assertEquals(_, true))

}
