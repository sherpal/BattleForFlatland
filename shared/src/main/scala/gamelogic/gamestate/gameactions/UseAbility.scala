package gamelogic.gamestate.gameactions

import gamelogic.abilities.Ability
import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{CasterUsesAbility, GameStateTransformer}
import gamelogic.gamestate.{GameAction, GameState}

final case class UseAbility(
    id: GameAction.Id,
    time: Long,
    casterId: Entity.Id,
    useId: Ability.UseId,
    ability: Ability
) extends GameAction {

  /**
    * Important: the time of the ability <strong>has</strong> to be changed by the game master when it's actually
    * applied
    */
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    new CasterUsesAbility(ability)

  /** This is going to be handled by the Server itself, and hence  */
  def isLegal(gameState: GameState): Option[String] = gameState.withAbilityEntitiesById(casterId) match {
    case None => Some(s"Entity ${casterId} does not exist, or is not an entity with abilities")
    case _    => None
  }

  def changeId(newId: Id): GameAction = copy(id = newId)
}
