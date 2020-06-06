package gamelogic.gamestate

import gamelogic.buffs.{Buff, PassiveBuff, TickerBuff}
import gamelogic.entities._
import gamelogic.entities.boss.BossEntity
import gamelogic.entities.classes.PlayerClass
import gamelogic.entities.movingstuff.PentagonBullet
import gamelogic.entities.staticstuff.Obstacle
import models.syntax.Pointed

/**
  * A [[gamelogic.gamestate.GameState]] has the complete knowledge of everything that exists in the game.
  * Having an instance of a GameState is enough to have all information about the game at that particular moment in time.
  *
  * An [[gamelogic.entities.Entity]] can cast only one spell at a time, hence the map.
  * An entity can have any number of [[gamelogic.buffs.Buff]] on it. We map the entity id to each of the buffs
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
    pentagonBullets: Map[Entity.Id, PentagonBullet],
    castingEntityInfo: Map[Entity.Id, EntityCastingInfo],
    passiveBuffs: Map[Entity.Id, Map[Buff.Id, PassiveBuff]],
    tickerBuffs: Map[Entity.Id, Map[Buff.Id, TickerBuff]],
    private val obstacles: Map[Entity.Id, Obstacle]
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
      time + delay - castingInfo.startedTime <= castingInfo.castingTime
  }

  /**
    * Applies the effects of all the current passive buffs to the given actions.
    *
    * Each passive buff takes an action and returns a list of actions caused by the changed.
    * We apply all changes to all cumulative actions that happen.
    *
    * This has one important consequence: it is not commutative on the set of actions.
    * This *should* not be an issue, because it should be in the contract of changer that they should not violate
    * commutativity. Nonetheless, this is something to keep in mind for the future. Perhaps a passive buff could
    * also have a priority.
    */
  def applyActionChangers(action: GameAction): List[GameAction] =
    passiveBuffs.valuesIterator
      .flatMap(_.valuesIterator)
      .map(buff => buff.actionTransformer(_))
      .foldLeft(List(action))(_.flatMap(_))

  /** See other overloaded methods. */
  def applyActionChangers(actions: List[GameAction]): List[GameAction] = {
    val changers = passiveBuffs.valuesIterator.flatMap(_.valuesIterator).toList
    actions.flatMap(
      action =>
        changers.foldLeft(List(action)) { (as: List[GameAction], changer: PassiveBuff) =>
          as.flatMap(changer.actionTransformer)
        }
    )
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
      .orElse(pentagonBullets.get(entityId))
      .orElse(obstacles.get(entityId))

  def allTargetableEntities: Iterator[MovingBody with LivingEntity] =
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

  def allLivingEntities: Iterator[LivingEntity with MovingBody] =
    players.valuesIterator ++ bosses.valuesIterator ++ dummyMobs.valuesIterator

  def withThreatEntityById(entityId: Entity.Id): Option[WithThreat] =
    bosses.get(entityId)

  def withPositionEntityById(entityId: Entity.Id): Option[WithPosition] =
    players.get(entityId).orElse(bosses.get(entityId)).orElse(dummyMobs.get(entityId)).orElse(obstacles.get(entityId))

  def movingBodyEntityById(entityId: Entity.Id): Option[MovingBody] =
    players
      .get(entityId)
      .orElse(bosses.get(entityId))
      .orElse(dummyMobs.get(entityId))
      .orElse(pentagonBullets.get(entityId))

  def livingEntityAndMovingBodyById(entityId: Entity.Id): Option[MovingBody with LivingEntity] =
    for {
      _ <- movingBodyEntityById(entityId)
      entity <- livingEntityById(entityId)
    } yield entity.asInstanceOf[MovingBody with LivingEntity] // this is ugly as hell. todo: think about it

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

  def obstaclesLike: Iterator[Body] = obstacles.valuesIterator

  /**
    * We build an interface on top of accessing and changing obstacles because the implementation could be different than
    * a simple Map in the future. Perhaps something like a QuadTree would be better for performances.
    */
  def withObstacle(obstacle: Obstacle): GameState      = copy(obstacles = obstacles + (obstacle.id -> obstacle))
  def removeObstacle(obstacleId: Entity.Id): GameState = copy(obstacles = obstacles - obstacleId)
}

object GameState {

  def empty: GameState = Pointed[GameState].unit

}
