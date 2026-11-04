package gamelogic.gamestate.gameactions.classes.square

import gamelogic.buffs.Buff
import gamelogic.buffs.abilities.classes.{Guarded, Guarding}
import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithBuff}
import gamelogic.gamestate.{GameAction, GameState}

/** Puts a [[Guarding]] buff on `bearerId`, to protect `guardedId`, and a [[Guarded]] buff on
  * `guardedId`, to show it.
  */
final case class PutGuarding(
    id: GameAction.Id,
    time: Long,
    buffId: Buff.Id,
    bearerId: Entity.Id,
    guardedId: Entity.Id,
    guardedBuffId: Buff.Id
) extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    WithBuff(Guarding(buffId, bearerId, bearerId, time, guardedId, guardedBuffId)) ++
      WithBuff(Guarded(guardedBuffId, guardedId, bearerId, time, buffId))

  def isLegal(gameState: GameState): Option[String] =
    (gameState.livingEntityById(bearerId), gameState.livingEntityById(guardedId)) match {
      case (Some(_), Some(_)) => None
      case _ => Some(s"Entity $bearerId or $guardedId does not exist, or is not a living entity")
    }

  def changeId(newId: Id): GameAction = copy(id = newId)
}
