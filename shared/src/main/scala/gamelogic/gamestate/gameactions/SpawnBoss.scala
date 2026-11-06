package gamelogic.gamestate.gameactions

import gamelogic.entities.Entity
import gamelogic.entities.boss.BossEntity
import gamelogic.gamestate.GameAction.EntityCreatorAction
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithEntity}
import gamelogic.gamestate.{GameAction, GameState}
import services.i18n.I18n

final case class SpawnBoss(id: GameAction.Id, time: Long, entityId: Entity.Id, bossName: String)
    extends GameAction
    with EntityCreatorAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    BossEntity
      .maybeInitialBossByName(bossName, entityId, time)
      .fold(
        GameStateTransformer.identityTransformer
      ) { initialBoss =>
        new WithEntity(initialBoss, time)
      }

  def isLegal(gameState: GameState)(using i18n: I18n): Option[String] =
    Option.unless(BossEntity.bossExists(bossName))(i18n.bossDoesNotExist(bossName))

  def changeId(newId: GameAction.Id): GameAction = copy(id = newId)
}
