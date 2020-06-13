package gamelogic.gamestate.statetransformers

import gamelogic.entities.boss.BossEntity
import gamelogic.entities.boss.boss102.DamageZone
import gamelogic.entities.classes.PlayerClass
import gamelogic.entities.movingstuff.PentagonBullet
import gamelogic.entities.staticstuff.Obstacle
import gamelogic.entities.{DummyMob, Entity}
import gamelogic.gamestate.GameState

final class WithEntity(entity: Entity, time: Long) extends GameStateTransformer {
  def apply(gameState: GameState): GameState = entity match {
    case entity: PlayerClass =>
      gameState.copy(time = time, players = gameState.players + (entity.id -> entity))
    case entity: BossEntity =>
      gameState.copy(time = time, bosses = gameState.bosses + (entity.id -> entity))
    case entity: DummyMob =>
      gameState.copy(time = time, dummyMobs = gameState.dummyMobs + (entity.id -> entity))
    case entity: PentagonBullet =>
      gameState.copy(time = time, pentagonBullets = gameState.pentagonBullets + (entity.id -> entity))
    case entity: Obstacle =>
      gameState.withObstacle(entity)
    case entity: DamageZone =>
      gameState.copy(time = time, otherEntities = gameState.otherEntities + (entity.id -> entity))
  }
}
