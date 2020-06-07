package gamelogic.gamestate.statetransformers

import gamelogic.entities.boss.BossEntity
import gamelogic.entities.classes.PlayerClass
import gamelogic.entities.movingstuff.PentagonBullet
import gamelogic.entities.staticstuff.Obstacle
import gamelogic.entities.{DummyMob, Entity, SimpleBulletBody}
import gamelogic.gamestate.GameState

final class RemoveEntityTransformer(entityId: Entity.Id, time: Long) extends GameStateTransformer {
  def apply(gameState: GameState): GameState =
    gameState.entityById(entityId).fold(gameState) {
      case _: BossEntity =>
        gameState.copy(time = time, bosses = gameState.bosses - entityId)
      case _: PlayerClass =>
        gameState.copy(time = time, players = gameState.players - entityId)
      case _: DummyMob =>
        gameState.copy(time = time, dummyMobs = gameState.dummyMobs - entityId)
      case _: SimpleBulletBody =>
        gameState.copy(time = time, simpleBullets = gameState.simpleBullets - entityId)
      case _: PentagonBullet =>
        gameState.copy(time = time, pentagonBullets = gameState.pentagonBullets - entityId)
      case _: Obstacle =>
        gameState.removeObstacle(entityId, time)
      case entity =>
        println(s"Unknown entity class was not removed: $entity")
        gameState
    }
}
