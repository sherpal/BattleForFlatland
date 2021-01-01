package gamelogic.gamestate.gameactions

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, HoT}
import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithBuff}
import gamelogic.gamestate.{GameAction, GameState}

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
    _appearanceTime: Long,
    resourceIdentifier: ResourceIdentifier
) extends GameAction {

  def createGameStateTransformer(gameState: GameState): GameStateTransformer = new WithBuff(
    HoT.constantHot(
      time,
      targetId,
      _buffId,
      _duration,
      _tickRate,
      healOnTick,
      _sourceId,
      _appearanceTime,
      resourceIdentifier
    )
  )

  def isLegal(gameState: GameState): Option[String] = gameState.livingEntityById(targetId) match {
    case None => Some(s"Entity $targetId does not exist, or is not a living entity")
    case _    => None
  }

  def changeId(newId: Id): GameAction = copy(id = newId)
}
