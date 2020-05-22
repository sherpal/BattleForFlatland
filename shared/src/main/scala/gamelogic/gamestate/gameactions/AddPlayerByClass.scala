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
  def createGameStateTransformer(gameState: GameState): GameStateTransformer = new WithEntity(
    playerClass match {
      case PlayerClasses.Square =>
        Square(
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
          Square.initialResourceAmount,
          Square.initialResourceAmount.amount,
          playerName
        )
      case PlayerClasses.Hexagon =>
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
        )
    }
  )

  def isLegal(gameState: GameState): Boolean = !gameState.players.isDefinedAt(entityId)

  def changeId(newId: Id): GameAction = copy(id = newId)
}
