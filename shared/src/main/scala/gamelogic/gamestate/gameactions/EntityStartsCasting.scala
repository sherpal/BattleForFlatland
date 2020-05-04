package gamelogic.gamestate.gameactions

import gamelogic.abilities.Ability
import gamelogic.entities.EntityCastingInfo
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{EntityStartsCastingTransformer, GameStateTransformer}
import gamelogic.physics.Complex

/** An entity starts casting now. */
final case class EntityStartsCasting(id: GameAction.Id, time: Long, ability: Ability) extends GameAction {

  /**
    * Testing that the entity is not already casting something, and that it exists.
    * Testing in that order because the first one is most likely the one which is going to fail.
    */
  def isLegal(gameState: GameState): Boolean = true
//    !gameState.entityIsCasting(ability.casterId) &&
//      gameState.withAbilityEntitiesById(ability.casterId).exists(_.canUseAbility(ability.abilityId, time))

  def changeId(newId: Id): GameAction = copy(id = newId)

  /**
    * If the ability has no casting time, then the [[GameStateTransformer]] is the one of the
    * [[gamelogic.gamestate.gameactions.UseAbility]] instead.
    */
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    if (ability.castingTime <= 0)
      UseAbility(id, time, ability.casterId, 0L, ability).createGameStateTransformer(gameState)
    else
      new EntityStartsCastingTransformer(
        EntityCastingInfo(
          ability.casterId,
          gameState.withAbilityEntitiesById(ability.casterId).map(_.pos).getOrElse(Complex.zero),
          time,
          ability
        )
      )
}
