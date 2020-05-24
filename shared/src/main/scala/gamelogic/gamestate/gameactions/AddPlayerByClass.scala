package gamelogic.gamestate.gameactions

import gamelogic.entities.Entity
import gamelogic.entities.classes.{Constants, Hexagon, Square}
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithEntity}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import models.bff.outofgame.PlayerClasses

final case class AddPlayerByClass(
    id: GameAction.Id,
    time: Long,
    entityId: Entity.Id,
    position: Complex,
    playerClass: PlayerClasses,
    colour: Int,
    playerName: String
) extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    playerClass match {
      case PlayerClasses.Square =>
        new WithEntity(
          Square(
            entityId,
            time,
            position,
            0.0,
            moving = false,
            0.0,
            Square.initialMaxLife,
            colour,
            Map(),
            Square.initialMaxLife,
            Constants.playerSpeed,
            Square.initialResourceAmount,
            Square.initialResourceAmount.amount,
            playerName
          ),
          time
        )
      case PlayerClasses.Hexagon =>
        new WithEntity(
          Hexagon(
            entityId,
            time,
            position,
            0.0,
            moving = false,
            0.0,
            100,
            colour,
            Map(),
            100,
            Constants.playerSpeed,
            Hexagon.initialResourceAmount,
            Hexagon.initialResourceAmount.amount,
            playerName
          ),
          time
        )
    }

  def isLegal(gameState: GameState): Boolean = !gameState.players.isDefinedAt(entityId)

  def changeId(newId: Id): GameAction = copy(id = newId)
}
