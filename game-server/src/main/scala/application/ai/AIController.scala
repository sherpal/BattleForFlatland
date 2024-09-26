package application.ai

import gamelogic.entities.classes.PlayerClass
import gamelogic.entities.{Entity, MovingBody, WithPosition, WithThreat}
import gamelogic.gamestate.GameAction.EntityCreatorAction
import gamelogic.gamestate.gameactions.{ChangeTarget, EntityStartsCasting, MovingBodyMoves}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.physics.pathfinding.Graph
import application.ai.utils.findTarget

import scala.concurrent.duration.*
import gamelogic.abilities.Ability

/** The AIController trait contains all the boilerplate that you need for implementing an AI.
  *
  * The methods to implement are
  *   - `takeActions` method. It takes the current [[gamelogic.gamestate.GameState]] and a few
  *     pre-computed values. And it returns the list of actions that the AI wants to take given
  *     these inputs.
  *   - `getMe`: how to access this entity based on its id
  */
trait AIController[
    EntityType <: MovingBody & WithThreat & WithPosition,
    InitialAction <: EntityCreatorAction
] {

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
      maybeTarget: Option[PlayerClass],
      obstacleGraph: Graph
  ): Vector[GameAction]

  protected def getMe(gameState: GameState, entityId: Entity.Id): Option[EntityType]

  inline private def now = System.currentTimeMillis()

  def loopRate = AIManager.loopRate

  private var lastTimeStamp = now

  def computeActions(
      myId: Entity.Id,
      gameState: GameState,
      graphs: Double => Option[Graph]
  ): Vector[GameAction] =
    val startTime          = now
    val timeSinceLastFrame = now - lastTimeStamp
    val shouldRun          = timeSinceLastFrame > loopRate
    if shouldRun then
      getMe(gameState, myId).fold(Vector.empty) { me =>
        val startTime       = now
        val currentPosition = me.currentPosition(startTime)
        val maybeTarget     = findTarget(me, gameState)
        graphs(me.shape.radius) match {
          case Some(graph) =>
            val result = takeActions(
              gameState,
              me,
              currentPosition,
              startTime,
              timeSinceLastFrame,
              findTarget(me, gameState),
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

  /** Utility method usable by sub classes which will (maybe) determine what action(s) need to be
    * performed.
    *
    * @param maybeActions
    *   list of legal attack that could be used at the given time. The attack, if any, which will be
    *   performed correspond to the first defined element in the list.
    * @param maybeChangeTarget
    *   if the entity needs to change its target, this argument must be defined
    * @param maybeMove
    *   if the entity must move, this argument must be defined
    * @return
    */
  protected def useAbility(
      maybeActions: Vector[Option[EntityStartsCasting]],
      maybeChangeTarget: Option[ChangeTarget],
      maybeMove: Option[MovingBodyMoves]
  ): Vector[GameAction] = maybeActions.collectFirst { case Some(action) => action } match {
    case None => Vector(maybeChangeTarget, maybeMove).flatten
    case action =>
      Vector(
        maybeChangeTarget,
        maybeMove.map(_.copy(moving = false)),
        action
      ).flatten
  }

}
