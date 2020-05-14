package gamelogic.gamestate.gameactions

import gamelogic.buffs.{Buff, HoT, TickerBuff}
import gamelogic.entities.Entity
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithBuff}

/**
  * Adds or updates a [[gamelogic.buffs.HoT]] with constant healing to the given entity.
  */
final case class UpdateConstantHot(
    id: GameAction.Id,
    time: Long,
    targetId: Entity.Id,
    _buffId: Buff.Id,
    _duration: Long,
    _tickRate: Long,
    healOnTick: Double,
    _sourceId: Entity.Id,
    _appearanceTime: Long
) extends GameAction {

  def createGameStateTransformer(gameState: GameState): GameStateTransformer = new WithBuff(
    HoT.constantHot(time, targetId, _buffId, _duration, _tickRate, healOnTick, _sourceId, _appearanceTime)
  )

  def isLegal(gameState: GameState): Boolean = gameState.livingEntityById(targetId).isDefined

  def changeId(newId: Id): GameAction = copy(id = newId)
}
