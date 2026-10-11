package gamelogic.gamestate.gameactions.boss102

import gamelogic.entities.Entity
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.boss.boss102.BossHound
import gamelogic.gamestate.GameAction.{EntityCreatorAction, Id}
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithEntity}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex

final case class AddBossHound(id: GameAction.Id, time: Long, entityId: Entity.Id, position: Complex)
    extends GameAction
    with EntityCreatorAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer = new WithEntity(
    BossHound(
      entityId,
      time,
      position,
      Angle.zero,
      Angle.zero,
      BossHound.fullSpeed,
      moving = false,
      BossHound.houndMaxLife,
      Map(),
      entityId,
      Map()
    ),
    time
  )

  def isLegal(gameState: GameState): None.type = None

  def changeId(newId: Id): GameAction = copy(id = newId)
}
