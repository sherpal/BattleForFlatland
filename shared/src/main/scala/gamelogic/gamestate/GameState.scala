package gamelogic.gamestate

import gamelogic.buffs.{Buff, PassiveBuff, TickerBuff}
import gamelogic.entities._
import gamelogic.entities.boss.BossEntity
import gamelogic.entities.classes.PlayerClass
import models.syntax.Pointed

/**
  * A [[gamelogic.gamestate.GameState]] has the complete knowledge of everything that exists in the game.
  * Having an instance of a GameState is enough to have all information about the game at that particular moment in time.
  *
  * An [[gamelogic.entities.Entity]] can cast only one spell at a time, hence the map.
  * An entity can have on it any number of [[gamelogic.buffs.Buff]] on it. We map the entity id to each of the buffs
  * attached to it, so that we can easily find them, and it's going to be more efficient when updating someones buffs.
  *
  * Ticker and passive buffs have very different behaviours, and that's why we separate them below.
  *
  * @param time in millis
  */
final case class GameState(
    time: Long,
    startTime: Option[Long],
    endTime: Option[Long],
    players: Map[Entity.Id, PlayerClass],
    bosses: Map[Entity.Id, BossEntity],
    dummyMobs: Map[Entity.Id, DummyMob],
    simpleBullets: Map[Entity.Id, SimpleBulletBody],
    castingEntityInfo: Map[Entity.Id, EntityCastingInfo],
    passiveBuffs: Map[Entity.Id, Map[Buff.Id, PassiveBuff]],
    tickerBuffs: Map[Entity.Id, Map[Buff.Id, TickerBuff]]
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
  def entityIsCasting(entityId: Entity.Id, delay: Long): Boolean = castingEntityInfo.get(entityId).fold(false) {
    castingInfo =>
      time + delay - castingInfo.startedTime >= castingInfo.castingTime
  }

  /**
    * Returns whether the two entities given ids are in the same team.
    * If either of the entities does not exist, returns None instead.
    */
  def areTheyFromSameTeam(entityId1: Entity.Id, entityId2: Entity.Id): Option[Boolean] =
    for {
      entity1 <- entityById(entityId1)
      entity2 <- entityById(entityId2)
    } yield entity1.teamId == entity2.teamId

  // todo: look for other kind of entity in all of methods below.
  def entityById(entityId: Entity.Id): Option[Entity] =
    players
      .get(entityId)
      .orElse(bosses.get(entityId))
      .orElse(dummyMobs.get(entityId))
      .orElse(simpleBullets.get(entityId))

  def allTargettableEntities: Iterator[MovingBody with LivingEntity] =
    players.valuesIterator ++ bosses.valuesIterator ++ dummyMobs.valuesIterator

  // Is there something better?
  def withAbilityEntitiesById(entityId: Entity.Id): Option[WithAbilities] =
    players
      .get(entityId)
      .orElse(bosses.get(entityId))

  def livingEntityById(entityId: Entity.Id): Option[LivingEntity] =
    players
      .get(entityId)
      .orElse(bosses.get(entityId))
      .orElse(dummyMobs.get(entityId))

  def withThreatEntityById(entityId: Entity.Id): Option[WithThreat] =
    bosses.get(entityId)

  def withPositionEntityById(entityId: Entity.Id): Option[WithPosition] =
    players.get(entityId).orElse(bosses.get(entityId)).orElse(dummyMobs.get(entityId))

  def movingBodyEntityById(entityId: Entity.Id): Option[MovingBody] =
    players.get(entityId).orElse(bosses.get(entityId)).orElse(dummyMobs.get(entityId))

  def withTargetEntityById(entityId: Entity.Id): Option[WithTarget] =
    bosses.get(entityId)

  def buffById(entityId: Entity.Id, buffId: Buff.Id): Option[Buff] =
    tickerBuffs
      .get(entityId)
      .flatMap(_.get(buffId))
      .orElse(passiveBuffs.get(entityId).flatMap(_.get(buffId)))

  def allBuffs: Iterable[Buff] = tickerBuffs.flatMap(_._2).values ++ passiveBuffs.flatMap(_._2).values
  def allBuffsOfEntity(entityId: Entity.Id): Iterator[Buff] =
    tickerBuffs.getOrElse(entityId, Map()).valuesIterator ++
      passiveBuffs.getOrElse(entityId, Map()).valuesIterator
}

object GameState {

  def empty: GameState = Pointed[GameState].unit

}
