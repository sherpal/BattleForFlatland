package gamelogic.gamestate

import gamelogic.entities.{DummyLivingEntity, Entity, EntityCastingInfo, SimpleBulletBody, WithAbilities}

/**
  * A [[gamelogic.gamestate.GameState]] has the complete knowledge of everything that exists in the game.
  * Having an instance of a GameState is enough to have all information about the game at that particular moment in time.
  *
  * An [[gamelogic.entities.Entity]] can cast only one spell at a time, hence the map.
  *
  * @param time in millis
  */
final case class GameState(
    time: Long,
    startTime: Option[Long],
    endTime: Option[Long],
    players: Map[Entity.Id, DummyLivingEntity],
    simpleBullets: Map[Entity.Id, SimpleBulletBody],
    castingEntityInfo: Map[Entity.Id, EntityCastingInfo]
) {

  def started: Boolean = startTime.isDefined
  def ended: Boolean   = endTime.isDefined

  /**
    * Applies the effects of all the actions in the list to this [[GameState]].
    * Actions are assumed to be ordered in time already.
    */
  def applyActions(actions: List[GameAction]): GameState = actions.foldLeft(this) { (currentGameState, nextAction) =>
    nextAction(currentGameState)
  }

  def isLegalAction(action: GameAction): Boolean = action.isLegal(gameState = this)

  def entityIsCasting(entityId: Entity.Id): Boolean = castingEntityInfo.isDefinedAt(entityId)

  def withAbilityEntitiesById(entityId: Entity.Id): Option[WithAbilities] =
    players.get(entityId) // todo: look for other kind of entity

}

object GameState {

  def initialGameState(time: Long): GameState = GameState(time, None, None, Map(), Map(), Map())

}
