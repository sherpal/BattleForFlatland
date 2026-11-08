package gamelogic.gamestate.gameactions

import gamelogic.buffs.MovementSpeedModifier
import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithEntity}
import gamelogic.gamestate.{GameAction, GameState}
import services.i18n.I18n

/** Gives the player its current speed (see [[MovementSpeedModifier]]), from where it is now.
  *
  * Emitted right after a [[MovementSpeedModifier]] appears or disappears, so that a player moving
  * in a straight line slows down (or speeds up again) without waiting for its next movement.
  */
final case class RefreshMovementSpeed(id: GameAction.Id, time: Long, entityId: Entity.Id)
    extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    gameState.players.get(entityId).fold(GameStateTransformer.identityTransformer) { player =>
      WithEntity(
        player.move(
          time,
          player.currentPosition(time),
          player.direction,
          player.rotation,
          MovementSpeedModifier.playerSpeed(gameState, player),
          player.moving
        ),
        time
      )
    }

  def isLegal(gameState: GameState)(using I18n): None.type = None

  def changeId(newId: Id): RefreshMovementSpeed = copy(id = newId)
}
