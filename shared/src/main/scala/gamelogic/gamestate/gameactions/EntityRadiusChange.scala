package gamelogic.gamestate.gameactions

import gamelogic.gamestate.GameAction
import gamelogic.gamestate.GameState
import gamelogic.gamestate.statetransformers.GameStateTransformer
import gamelogic.gamestate.GameState
import gamelogic.entities.Entity
import gamelogic.entities.WithChangingRadius
import gamelogic.gamestate.statetransformers.WithEntity

final case class EntityRadiusChange(id: GameAction.Id, time: Long, entityId: Entity.Id, radius: Double)
    extends GameAction {

  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    new GameStateTransformer {
      def apply(gs: GameState): GameState =
        gs.entities
          .get(entityId)
          .collect { case withRadius: WithChangingRadius => withRadius }
          .map(_.changeRadius(radius))
          .fold(gs)(new WithEntity(_, time)(gs))
    }

  def isLegal(gameState: GameState): Option[String] = Option.empty

  def changeId(newId: GameAction.Id): GameAction = copy(id = newId)

}
