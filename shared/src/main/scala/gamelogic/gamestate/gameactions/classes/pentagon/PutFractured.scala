package gamelogic.gamestate.gameactions.classes.pentagon

import gamelogic.buffs.Buff
import gamelogic.buffs.abilities.classes.Fractured
import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithBuff}
import gamelogic.gamestate.{GameAction, GameState}
import services.i18n.I18n

/** Puts a [[Fractured]] buff on `bearerId`, from `sourceId`, lasting `duration`. */
final case class PutFractured(
    id: GameAction.Id,
    time: Long,
    buffId: Buff.Id,
    bearerId: Entity.Id,
    sourceId: Entity.Id,
    duration: Long
) extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    WithBuff(Fractured(buffId, bearerId, sourceId, time, duration))

  def isLegal(gameState: GameState)(using i18n: I18n): Option[String] =
    gameState.livingEntityById(bearerId) match {
      case None => Some(i18n.entityIsNotLiving(bearerId))
      case _ => None
    }

  def changeId(newId: Id): GameAction = copy(id = newId)
}
