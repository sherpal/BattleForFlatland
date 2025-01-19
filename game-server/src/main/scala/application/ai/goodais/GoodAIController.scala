package application.ai.goodais

import gamelogic.entities.MovingBody
import gamelogic.entities.WithPosition
import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import gamelogic.entities.Entity
import gamelogic.physics.pathfinding.Graph
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.gameactions.*
import application.ai.GoodAIManager
import models.bff.outofgame.gameconfig.PlayerName
import gamelogic.buffs.Buff
import gamelogic.abilities.Ability
import scala.reflect.ClassTag

trait GoodAIController[
    EntityType <: MovingBody & WithPosition
] {

  val name: PlayerName.AIPlayerName

  val entityId: Entity.Id

  val classTag: ClassTag[EntityType]

  private given ClassTag[EntityType] = classTag

  extension (maybeAbility: Option[Ability]) {
    def startCasting: Option[EntityStartsCasting] = maybeAbility.map(ability =>
      EntityStartsCasting(GameAction.Id.zero, ability.time, ability.castingTime, ability)
    )
  }

  protected def takeActions(
      currentGameState: GameState,
      me: EntityType,
      currentPosition: Complex,
      startTime: Long,
      timeSinceLastFrame: Long,
      obstacleGraph: Graph
  ): Vector[GameAction]

  protected def getMe(gameState: GameState): Option[EntityType] =
    gameState.players.get(entityId).collect { case entity: EntityType =>
      entity
    }

  inline private def now = System.currentTimeMillis()

  def loopRate = GoodAIManager.loopRate

  private var lastTimeStamp = now

  def computeActions(
      gameState: GameState,
      graphs: Double => Option[Graph]
  ): Vector[GameAction] =
    val startTime          = now
    val timeSinceLastFrame = now - lastTimeStamp
    val shouldRun          = timeSinceLastFrame > loopRate
    if shouldRun then
      getMe(gameState).fold(Vector.empty) { me =>
        val startTime       = now
        val currentPosition = me.currentPosition(startTime)
        graphs(me.shape.radius) match {
          case Some(graph) =>
            val result = takeActions(
              gameState,
              me,
              currentPosition,
              startTime,
              timeSinceLastFrame,
              graph
            )
            lastTimeStamp = startTime
            result
          case None =>
            println(s"[warn] There is no graph for me. My radius is ${me.shape.radius}")
            Vector.empty
        }
      }
    else Vector.empty

    /** Moves as a straight line to the target position.
      *
      * @param time
      *   current time for the movement action
      * @param me
      *   instance of myself
      * @param currentPosition
      *   position that I have currently
      * @param targetPosition
      *   position where I want to go
      * @param travelledDistance
      *   distance travelled since last time
      * @return
      *   list of [[GameAction]] (actually only one) to perform in order to go there
      */
  def preGameMovement(
      time: Long,
      me: EntityType,
      currentPosition: Complex,
      targetPosition: Complex,
      travelledDistance: Double
  ): Vector[GameAction] = {
    val distanceToTarget = me.pos.distanceTo(targetPosition)

    if travelledDistance > distanceToTarget || distanceToTarget < 3 then {
      Vector(
        MovingBodyMoves(
          GameAction.Id.dummy,
          time,
          me.id,
          targetPosition,
          me.direction,
          me.rotation,
          me.speed,
          moving = false
        )
      )
    } else {
      val direction = (targetPosition - currentPosition).arg
      Vector(
        MovingBodyMoves(
          GameAction.Id.dummy,
          time,
          me.id,
          currentPosition,
          direction,
          direction,
          me.speed,
          moving = true
        )
      )
    }

  }

  /** Returns a triplet containing information about the current and past positions of the entity
    *
    * @param time
    *   current time (of the loop)
    * @param me
    *   instance of the entity type this controller controls
    * @return
    *   the previous position of the AI (in last loop) and the current position (now)
    */
  final def someDistanceInfo(time: Long, me: EntityType): (Complex, Complex, Double) = {
    val previousPosition = me.pos
    val currentPosition  = me.currentPosition(time)

    (previousPosition, currentPosition, previousPosition.distanceTo(currentPosition))
  }

  final def buffsOnMe(gameState: GameState, me: EntityType): List[Buff] =
    gameState.allBuffsOfEntity(me.id).toList

  final def stopMoving(time: Long, me: EntityType, currentPosition: Complex, rotation: Double) =
    Option.when(me.moving)(
      MovingBodyMoves(
        GameAction.Id.dummy,
        time,
        me.id,
        currentPosition,
        rotation,
        rotation,
        me.speed,
        moving = false
      )
    )
}
