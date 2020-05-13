package gamelogic.gamestate

import gamelogic.buffs.Buff
import gamelogic.entities._

/**
  * A [[gamelogic.gamestate.GameState]] has the complete knowledge of everything that exists in the game.
  * Having an instance of a GameState is enough to have all information about the game at that particular moment in time.
  *
  * An [[gamelogic.entities.Entity]] can cast only one spell at a time, hence the map.
  * An entity can have on it any number of [[gamelogic.buffs.Buff]] on it. We map the entity id to each of the buffs
  * attached to it, so that we can easily find them, and it's going to be more efficient when updating someones buffs.
  *
  * @param time in millis
  */
final case class GameState(
    time: Long,
    startTime: Option[Long],
    endTime: Option[Long],
    players: Map[Entity.Id, DummyLivingEntity],
    dummyMobs: Map[Entity.Id, DummyMob],
    simpleBullets: Map[Entity.Id, SimpleBulletBody],
    castingEntityInfo: Map[Entity.Id, EntityCastingInfo],
    buffs: Map[Entity.Id, Map[Buff.Id, Buff]]
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

  def entityIsCasting(entityId: Entity.Id): Boolean = entityIsCasting(entityId, 0L)
  def entityIsCasting(entityId: Entity.Id, delay: Long): Boolean = castingEntityInfo.get(entityId).fold(true) {
    castingInfo =>
      time + delay - castingInfo.startedTime >= castingInfo.castingTime
  }

  def withAbilityEntitiesById(entityId: Entity.Id): Option[WithAbilities] =
    players.get(entityId) // todo: look for other kind of entity

  def livingEntityById(entityId: Entity.Id): Option[LivingEntity] = // todo: add other kinds of entity
    players.get(entityId).orElse(dummyMobs.get(entityId))

  def withThreatEntityById(entityId: Entity.Id): Option[WithThreat] = None // todo: add other kinds of entity
}

object GameState {

  def empty: GameState = GameState(0L, None, None, Map(), Map(), Map(), Map(), Map())

}
