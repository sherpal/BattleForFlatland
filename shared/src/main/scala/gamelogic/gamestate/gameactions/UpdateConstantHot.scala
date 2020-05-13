package gamelogic.gamestate.gameactions

import gamelogic.buffs.{Buff, HoT}
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
    new HoT {
      val buffId: Id = _buffId

      val sourceId: Id = _sourceId

      def healPerTick(timeSinceBeginning: Id): Double = healOnTick

      val tickRate: Long       = _tickRate
      val bearerId: Entity.Id  = targetId
      val duration: Long       = _duration
      val appearanceTime: Long = _appearanceTime
      val lastTickTime: Long   = time // should a ticker tick when it pops? I don't think so
    }
  )

  def isLegal(gameState: GameState): Boolean = gameState.livingEntityById(targetId).isDefined

  def changeId(newId: Id): GameAction = copy(id = newId)
}
