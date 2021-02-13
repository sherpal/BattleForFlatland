package game.ai.boss

import game.ai.utils._
import scala.reflect.ClassTag
import gamelogic.gamestate.GameAction.EntityCreatorAction
import gamelogic.entities.MovingBody
import gamelogic.entities.WithThreat
import gamelogic.entities.WithPosition
import gamelogic.entities.classes.PlayerClass
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.physics.pathfinding.Graph
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import gamelogic.gamestate.gameactions.EntityStartsCasting
import gamelogic.abilities.Ability
import gamelogic.entities.WithTarget
import gamelogic.entities.WithAbilities

/**
  * Generates an [[AIController]] with a simple (but quite generic) behaviour, based on
  * - a melee range
  * - a full speed
  * - a way to potentially create actions based on the [[GameState]], the given EntityType and the current time.
  */
trait SimpleAIController[
    EntityType <: MovingBody with WithThreat with WithPosition with WithTarget with WithAbilities,
    InitialAction <: EntityCreatorAction
] extends AIController[EntityType, InitialAction] {

  def meleeRange: Double
  def fullSpeed: Double

  def actions(gameState: GameState, me: EntityType, time: Long): List[Option[EntityStartsCasting]]

  val classTag: ClassTag[EntityType]

  private implicit def ct: ClassTag[EntityType] = classTag

  protected final def takeActions(
      currentGameState: GameState,
      me: EntityType,
      currentPosition: Complex,
      startTime: Long,
      lastTimeStamp: Long,
      maybeTarget: Option[PlayerClass],
      obstacleGraph: Graph
  ): List[GameAction] =
    Option
      .unless(currentGameState.entityIsCasting(me.id))(maybeTarget)
      .flatten
      .map { target =>
        // If the boss is casting, he doesn't do anything else.
        // If the boss has no target, the only possibility is that all players are dead.
        // In that case, the game either has not started yet or it will end very soon so we don't do anything.

        /** changing target */
        val maybeChangeTarget = changeTarget(me, target.id, startTime)

        val elapsedSinceLastFrame = startTime - lastTimeStamp

        val maybeMove = aiMovementToTargetWithGraph(
          me.id,
          startTime,
          elapsedSinceLastFrame,
          currentPosition,
          me.shape.radius,
          target.currentPosition(startTime + 100),
          meleeRange,
          fullSpeed,
          fullSpeed / 4,
          me.speed,
          me.moving,
          me.rotation,
          obstacleGraph,
          position => !currentGameState.obstacles.valuesIterator.exists(_.collidesShape(me.shape, position, 0, 0))
        )

        useAbility(
          actions(currentGameState, me, startTime),
          maybeChangeTarget,
          maybeMove
        )
      }
      .getOrElse(Nil)

  protected final def getMe(gameState: GameState, entityId: Entity.Id): Option[EntityType] =
    gameState.entities.get(entityId).collect { case entity: EntityType => entity }

}
