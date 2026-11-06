package gamelogic.gamestate.gameactions

import gamelogic.buffs.Buff.StackedBuffIdentifier
import gamelogic.buffs.{Buff, StackedBuffs}
import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithBuff}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.PosInt
import services.i18n.I18n

/** Puts the [[StackedBuffs]] with the specified [[StackedBuffIdentifier]] and number of stacks.
  *
  * @see
  *   [[StackedBuffs]].
  */
final case class PutStackedBuff(
    id: GameAction.Id,
    time: Long,
    buffId: Buff.Id,
    bearerId: Entity.Id,
    sourceId: Entity.Id,
    appearanceTime: Long,
    resourceIdentifier: StackedBuffIdentifier,
    stacks: PosInt
) extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    gameState.entityById(bearerId).fold(GameStateTransformer.identityTransformer) { _ =>
      val newBuff =
        StackedBuffs(resourceIdentifier, buffId, bearerId, sourceId, appearanceTime, stacks)
      WithBuff(newBuff)
    }

  def isLegal(gameState: GameState)(using i18n: I18n): Option[String] =
    gameState.entityById(bearerId) match {
      case None => Some(i18n.entityDoesNotExist(bearerId))
      case _ => None
    }

  def changeId(newId: Id): GameAction = copy(id = newId)
}
