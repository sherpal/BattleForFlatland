package gamelogic.gamestate.gameactions

import gamelogic.abilities.Ability
import gamelogic.entities.Entity
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{CasterUsesAbility, GameStateTransformer}

case class UseAbility(id: GameAction.Id, time: Long, casterId: Entity.Id, useId: Ability.UseId, ability: Ability)
    extends GameAction {

  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    new CasterUsesAbility(ability.copyWithNewTimeAndId(time, useId))

  /** This is going to be handled by the Server itself, and hence  */
  def isLegal(gameState: GameState): Boolean = gameState.withAbilityEntitiesById(casterId).isDefined

  def changeId(newId: Id): GameAction = copy(id = newId)
}
