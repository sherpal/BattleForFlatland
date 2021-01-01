package gamelogic.gamestate.gameactions

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, DoT}
import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithBuff}
import gamelogic.gamestate.{GameAction, GameState}

final case class PutConstantDot(
    id: GameAction.Id,
    time: Long,
    targetId: Entity.Id,
    sourceId: Entity.Id,
    damageOnTick: Double,
    duration: Long,
    tickRate: Long,
    buffId: Buff.Id,
    resourceIdentifier: ResourceIdentifier
) extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    gameState
      .livingEntityById(targetId)
      .fold(GameStateTransformer.identityTransformer) { _ =>
        new WithBuff(
          DoT.constantDot(time, targetId, buffId, duration, tickRate, damageOnTick, sourceId, time, resourceIdentifier)
        )
      }

  def isLegal(gameState: GameState): Option[String] = gameState.livingEntityById(targetId) match {
    case None => Some(s"Entity $targetId does not exist or is not a living entity")
    case _    => None
  }

  def changeId(newId: Id): GameAction = copy(id = newId)
}
