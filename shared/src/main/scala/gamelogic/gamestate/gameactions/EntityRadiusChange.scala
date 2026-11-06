package gamelogic.gamestate.gameactions

import gamelogic.entities.{Entity, WithChangingRadius}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithEntity}
import services.i18n.I18n

final case class EntityRadiusChange(
    id: GameAction.Id,
    time: Long,
    entityId: Entity.Id,
    radius: Double
) extends GameAction {

  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    (gs: GameState) =>
      gs.entities
        .get(entityId)
        .collect { case withRadius: WithChangingRadius => withRadius }
        .map(_.changeRadius(radius))
        .fold(gs)(new WithEntity(_, time)(gs))

  def isLegal(gameState: GameState)(using I18n): Option[String] = Option.empty

  def changeId(newId: GameAction.Id): GameAction = copy(id = newId)

}
