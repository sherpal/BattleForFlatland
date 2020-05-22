package gamelogic.gamestate.gameactions

import gamelogic.entities.Entity
import gamelogic.entities.boss.BossEntity
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithEntity}

final case class SpawnBoss(id: GameAction.Id, time: Long, entityId: Entity.Id, bossName: String) extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    BossEntity
      .maybeInitialBossByName(bossName, entityId, time)
      .fold(
        GameStateTransformer.identityTransformer
      ) { initialBoss =>
        new WithEntity(initialBoss)
      }

  def isLegal(gameState: GameState): Boolean = BossEntity.bossExists(bossName)

  def changeId(newId: GameAction.Id): GameAction = copy(id = newId)
}
