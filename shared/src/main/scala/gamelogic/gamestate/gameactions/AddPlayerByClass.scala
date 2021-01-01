package gamelogic.gamestate.gameactions

import gamelogic.entities.Entity
import gamelogic.entities.classes._
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
            Hexagon.initialMaxLife,
            colour,
            Map(),
            Hexagon.initialMaxLife,
            Constants.playerSpeed,
            Hexagon.initialResourceAmount,
            Hexagon.initialResourceAmount.amount,
            playerName
          ),
          time
        )
      case PlayerClasses.Triangle =>
        new WithEntity(
          Triangle(
            entityId,
            time,
            position,
            0.0,
            moving = false,
            0.0,
            Triangle.initialMaxLife,
            colour,
            Map(),
            Triangle.initialMaxLife,
            Constants.playerSpeed,
            Triangle.initialResourceAmount,
            Triangle.initialResourceAmount.amount,
            playerName
          ),
          time
        )
      case PlayerClasses.Pentagon =>
        new WithEntity(
          Pentagon(
            entityId,
            time,
            position,
            0.0,
            moving = false,
            0.0,
            Pentagon.initialMaxLife,
            colour,
            Map(),
            Pentagon.initialMaxLife,
            Constants.playerSpeed,
            Pentagon.initialResourceAmount,
            Pentagon.initialResourceAmount.amount,
            playerName
          ),
          time
        )
    }

  def isLegal(gameState: GameState): Option[String] =
    Option.when(gameState.players.isDefinedAt(entityId))("Player already exists")

  def changeId(newId: Id): GameAction = copy(id = newId)
}
