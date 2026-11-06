package gamelogic.gamestate.gameactions

import gamelogic.abilities.Ability
import gamelogic.entities.EntityCastingInfo
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{EntityStartsCastingTransformer, GameStateTransformer}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import services.i18n.I18n

/** An entity starts casting now. */
final case class EntityStartsCasting(
    id: GameAction.Id,
    time: Long,
    castingTime: Long,
    ability: Ability
) extends GameAction {

  /** Testing that the entity is not already casting something, and that it exists. Testing in that
    * order because the first one is most likely the one which is going to fail.
    *
    * If the ability is not legal, then returns Some the error message. Otherwise, returns None.
    */
  def isLegal(gameState: GameState)(using i18n: I18n): Option[String] =
    Option
      .when(gameState.entityIsCasting(ability.casterId))(i18n.alreadyCasting)
      .orElse(
        (for {
          caster <- gameState
            .withAbilityEntitiesById(ability.casterId)
            .toRight(i18n.entityDoesNotExist(ability.casterId))
          _ <- caster.canUseAbility(ability, time, gameState).toLeft(())
        } yield ()).swap.toOption
      )
      .orElse(ability.canBeCast(gameState, time))

  /** Checks whether the caster will be authorized to cast this ability in `delay` milliseconds.
    */
  def isLegalDelay(gameState: GameState, delay: Long)(using i18n: I18n): Option[String] =
    Option
      .when(gameState.entityIsCasting(ability.casterId, delay))(i18n.alreadyCasting)
      .orElse(
        (for {
          caster <- gameState
            .withAbilityEntitiesById(ability.casterId)
            .toRight(i18n.entityDoesNotExist(ability.casterId))
          _ <- caster.canUseAbility(ability, time + delay, gameState).toLeft(())
        } yield ()).swap.toOption
      )
      .orElse(ability.canBeCast(gameState, time + delay))

  def changeId(newId: Id): EntityStartsCasting = copy(id = newId)

  /** If the ability has no casting time, then the [[GameStateTransformer]] is the one of the
    * [[gamelogic.gamestate.gameactions.UseAbility]] instead.
    */
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    new EntityStartsCastingTransformer(
      EntityCastingInfo(
        ability.casterId,
        gameState.withAbilityEntitiesById(ability.casterId).map(_.pos).getOrElse(Complex.zero),
        time,
        castingTime,
        ability
      )
    )
}
