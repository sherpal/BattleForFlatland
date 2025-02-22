package gamelogic.gamestate

import gamelogic.buffs.{Buff, PassiveBuff, TickerBuff}
import gamelogic.entities.Entity.Id
import gamelogic.entities._
import gamelogic.entities.boss.BossEntity
import gamelogic.entities.classes.PlayerClass
import gamelogic.entities.movingstuff.PentagonBullet
import gamelogic.entities.staticstuff.Obstacle
import models.syntax.Pointed

import scala.reflect.ClassTag
import gamelogic.gameextras.{GameMarker, GameMarkerInfo}

/** A [[gamelogic.gamestate.GameState]] has the complete knowledge of everything that exists in the
  * game. Having an instance of a GameState is enough to have all information about the game at that
  * particular moment in time.
  *
  * An [[gamelogic.entities.Entity]] can cast only one spell at a time, hence the map. An entity can
  * have any number of [[gamelogic.buffs.Buff]] on it. We map the entity id to each of the buffs
  * attached to it, so that we can easily find them, and it's going to be more efficient when
  * updating someones buffs.
  *
  * Ticker and passive buffs have very different behaviours, and that's why we separate them below.
  *
  * @param time
  *   in millis
  */
final case class GameState(
    time: Long,
    startTime: Option[Long],
    endTime: Option[Long],
    castingEntityInfo: Map[Entity.Id, EntityCastingInfo],
    passiveBuffs: Map[Entity.Id, Map[Buff.Id, PassiveBuff]],
    tickerBuffs: Map[Entity.Id, Map[Buff.Id, TickerBuff]],
    entities: Map[Entity.Id, Entity],
    markersInfo: Map[GameMarker, GameMarkerInfo],
    deadPlayers: Map[Entity.Id, PlayerClass]
) {

  def started: Boolean = startTime.isDefined
  def ended: Boolean   = endTime.isDefined

  def playersWon: Boolean = ended && bosses.isEmpty

  /** Applies the effects of all the actions in the list to this [[GameState]]. Actions are assumed
    * to be ordered in time already.
    */
  def applyActions(actions: Vector[GameAction]): GameState = actions.foldLeft(this) {
    (currentGameState, nextAction) =>
      nextAction(currentGameState)
  }

  def isLegalAction(action: GameAction): Boolean = action.isLegal(gameState = this).isEmpty

  def entityIsCasting(entityId: Entity.Id): Boolean = entityIsCasting(entityId, 0L)
  def entityIsCasting(entityId: Entity.Id, delay: Long): Boolean =
    castingEntityInfo.get(entityId).fold(false) { castingInfo =>
      time + delay - castingInfo.startedTime <= castingInfo.castingTime
    }

  /** Applies the effects of all the current passive buffs to the given actions.
    *
    * Each passive buff takes an action and returns a list of actions caused by the changed. We
    * apply all changes to all cumulative actions that happen.
    *
    * This has one important consequence: it is not commutative on the set of actions. This *should*
    * not be an issue, because it should be in the contract of changer that they should not violate
    * commutativity. Nonetheless, this is something to keep in mind for the future. Perhaps a
    * passive buff could also have a priority.
    */
  def applyActionChangers(action: GameAction): List[GameAction] =
    passiveBuffs.valuesIterator
      .flatMap(_.valuesIterator)
      .map(buff => buff.actionTransformer(_))
      .foldLeft(List(action))(_.flatMap(_))

  /** See other overloaded methods. */
  def applyActionChangers(actions: List[GameAction]): List[GameAction] = {
    val changers = passiveBuffs.valuesIterator.flatMap(_.valuesIterator).toList
    actions.flatMap(action =>
      changers.foldLeft(List(action)) { (as: List[GameAction], changer: PassiveBuff) =>
        as.flatMap(changer.actionTransformer)
      }
    )
  }

  /** Returns whether no obstacle stands between the two given entity. Currently it's just a matter
    * of looping through all existing obstacles. Maybe there is a clever way.
    */
  def areTheyInSight(
      entityId1: Entity.Id,
      entityId2: Entity.Id,
      currentTime: Long
  ): Option[Boolean] =
    for {
      body1 <- bodyEntityById(entityId1)
      body2 <- bodyEntityById(entityId2)
      segment = (body1.currentPosition(currentTime), body2.currentPosition(currentTime))
    } yield !obstaclesLike.exists { body =>
      body.shape.intersectSegment(
        body.currentPosition(currentTime),
        body.rotation,
        segment._1,
        segment._2
      )
    }

  /** Returns whether the two entities given ids are in the same team. If either of the entities
    * does not exist, returns None instead.
    */
  def areTheyFromSameTeam(entityId1: Entity.Id, entityId2: Entity.Id): Option[Boolean] =
    for {
      entity1 <- entityById(entityId1)
      entity2 <- entityById(entityId2)
    } yield entity1.teamId == entity2.teamId

  /** Returns the entity with the given Id if it exists and if it is of type `T`. Returns None
    * otherwise.
    */
  def entityByIdAs[T <: Entity](entityId: Entity.Id)(implicit tag: ClassTag[T]): Option[T] =
    entities.get(entityId).collect(filterT[T])

  /** Creates a partial function which filters all [[gamelogic.entities.Entity]] of the specified
    * type `T`.
    */
  def filterT[T <: Entity](using ClassTag[T]): PartialFunction[Entity, T] = { case entity: T =>
    entity
  }

  /** Creates a Map from entity id to the corresponding entity, but only for those of type `T`.
    */
  def allTEntities[T <: Entity](using ClassTag[T]): Map[Entity.Id, T] =
    entities.collect { case (id, entity: T) => (id, entity) }

  lazy val players: Map[Id, PlayerClass]                   = allTEntities[PlayerClass]
  lazy val bosses: Map[Entity.Id, BossEntity]              = allTEntities[BossEntity]
  lazy val dummyMobs: Map[Entity.Id, DummyMob]             = allTEntities[DummyMob]
  lazy val simpleBullets: Map[Entity.Id, SimpleBulletBody] = allTEntities[SimpleBulletBody]
  lazy val pentagonBullets: Map[Entity.Id, PentagonBullet] = allTEntities[PentagonBullet]
  lazy val obstacles: Map[Entity.Id, Obstacle]             = allTEntities[Obstacle]

  def entityById(entityId: Entity.Id): Option[Entity] = entities.get(entityId)

  def allTargetableEntities: Iterator[MovingBody & LivingEntity] =
    entities.valuesIterator.collect(filterT[LivingEntity & MovingBody])

  def targetableEntityById(entityId: Entity.Id): Option[MovingBody & LivingEntity] =
    entities.get(entityId).collect(filterT[MovingBody & LivingEntity])

  // Is there something better?
  def withAbilityEntitiesById(entityId: Entity.Id): Option[WithAbilities] =
    entityByIdAs[WithAbilities](entityId)

  def livingEntityById(entityId: Entity.Id): Option[LivingEntity] =
    entityByIdAs[LivingEntity](entityId)

  def allLivingEntities: Iterator[LivingEntity & MovingBody] =
    entities.valuesIterator.collect { case entity: (LivingEntity & MovingBody) => entity }

  def withThreatEntityById(entityId: Entity.Id): Option[WithThreat] =
    entityByIdAs[WithThreat](entityId)

  def withPositionEntityById(entityId: Entity.Id): Option[WithPosition] =
    entityByIdAs[WithPosition](entityId)

  def movingBodyEntityById(entityId: Entity.Id): Option[MovingBody] =
    entityByIdAs[MovingBody](entityId)

  def bodyEntityById(entityId: Entity.Id): Option[Body] =
    movingBodyEntityById(entityId)

  def livingEntityAndMovingBodyById(entityId: Entity.Id): Option[MovingBody & LivingEntity] =
    entityByIdAs[MovingBody & LivingEntity](entityId)

  def withTargetEntityById(entityId: Entity.Id): Option[WithTarget] =
    entityByIdAs[WithTarget](entityId)

  def buffById(entityId: Entity.Id, buffId: Buff.Id): Option[Buff] =
    tickerBuffs
      .get(entityId)
      .flatMap(_.get(buffId))
      .orElse(passiveBuffs.get(entityId).flatMap(_.get(buffId)))

  def allBuffs: Iterable[Buff] =
    tickerBuffs.flatMap(_._2).values ++ passiveBuffs.flatMap(_._2).values
  def allBuffsOfEntity(entityId: Entity.Id): Iterator[Buff] =
    tickerBuffs.getOrElse(entityId, Map()).valuesIterator ++
      passiveBuffs.getOrElse(entityId, Map()).valuesIterator
  def hasBuffOfType[T <: Buff](entityId: Entity.Id)(using scala.reflect.Typeable[T]) =
    allBuffsOfEntity(entityId).exists {
      case _: T => true
      case _    => false
    }

  def obstaclesLike: Iterator[PolygonBody] = obstacles.valuesIterator
  def allObstacles: Iterator[Obstacle]     = obstacles.valuesIterator

  /** We build an interface on top of accessing and changing obstacles because the implementation
    * could be different than a simple Map in the future. Perhaps something like a QuadTree would be
    * better for performances.
    */
  def withObstacle(obstacle: Obstacle): GameState =
    copy(time = obstacle.time max time, entities = entities + (obstacle.id -> obstacle))
  def removeObstacle(obstacleId: Entity.Id, newTime: Long): GameState =
    copy(entities = entities - obstacleId, time = time max newTime)

  /** Adds the information about the given [[GameMarker]]. If the [[GameMarker]] is already present,
    * replaces it.
    */
  def withMarkerInfo(gameMarkerInfo: GameMarkerInfo): GameState =
    copy(markersInfo = markersInfo + (gameMarkerInfo.marker -> gameMarkerInfo))

  /** Removes the information about the given marker.
    */
  def removeMarkerInfo(marker: GameMarker): GameState =
    copy(markersInfo = markersInfo - marker)

  /** Returns the information about the specified [[GameMaker]]. */
  def maybeMarkerInfo(marker: GameMarker): Option[GameMarkerInfo] =
    markersInfo.get(marker)

}

object GameState {

  implicit def pointed: Pointed[GameState] = Pointed.factory(
    new GameState(0L, None, None, Map(), Map(), Map(), Map(), Map(), Map())
  )

  def empty: GameState = Pointed[GameState].unit

}
