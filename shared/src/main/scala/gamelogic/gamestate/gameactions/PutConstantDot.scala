package gamelogic.gamestate.gameactions

import gamelogic.buffs.{Buff, DoT}
import gamelogic.entities.Entity
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithBuff}

final case class PutConstantDot(
    id: GameAction.Id,
    time: Long,
    targetId: Entity.Id,
    sourceId: Entity.Id,
    damageOnTick: Double,
    duration: Long,
    tickRate: Long,
    buffId: Buff.Id
) extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    gameState
      .livingEntityById(targetId)
      .fold(GameStateTransformer.identityTransformer) { _ =>
        new WithBuff(DoT.constantDot(time, targetId, buffId, duration, tickRate, damageOnTick, sourceId, time))
      }

  def isLegal(gameState: GameState): Boolean = gameState.livingEntityById(targetId).isDefined

  def changeId(newId: Id): GameAction = copy(id = newId)
}
