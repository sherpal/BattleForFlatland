package game.ai.utils.pathfinders

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import game.ai.AIControllerMessage
import gamelogic.entities.{Entity, PolygonBody}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.pathfinding.{AIWanderGraph, Graph}
import gamelogic.physics.quadtree.ShapeQT

/**
  * A [[PathFinder]] actor is responsible for gathering information about some obstacles that pop in or are removed from
  * the game, and adjust the graph for path finding accordingly.
  *
  * To each [[PathFinder]] is associated a function identifying whether an action is responsible for adding an obstacle.
  * A basic [[PathFinder]] would be one taking only [[gamelogic.entities.staticstuff.Obstacle]].
  */
trait PathFinder {
  import PathFinder._

  def entityRadius: Double

  /**
    * Retrieves the new [[gamelogic.entities.PolygonBody]] created by the given action. If the action does not create
    * a [[gamelogic.entities.PolygonBody]] we care about, returns [[scala.None]] instead.
    */
  def maybeNewObstacle(action: GameAction, gameState: GameState): Option[PolygonBody]

  /**
    * Retrieves the id of the obstacle to remove from the given action. If the action is not an action removing the
    * obstacle, then [[scala.None]] can be returned instead.
    */
  def maybeRemoveObstacle(action: GameAction): Option[Entity.Id]

  /**
    * Retrieves from the current game state all the obstacles which should be considered by this [[PathFinder]].
    * The goal is to fill the information upon creation of this instance.
    */
  def obstaclesFromGameState(gameState: GameState): List[PolygonBody]

  protected case class PathFinderInfo(shapeQT: ShapeQT, subscribers: Set[ActorRef[AIControllerMessage.ObstacleGraph]]) {
    def addObstacles(obstacles: List[PolygonBody]): PathFinderInfo =
      copy(shapeQT = shapeQT ++ obstacles)

    def removeObstaclesByIds(ids: List[Entity.Id]): PathFinderInfo =
      copy(shapeQT = shapeQT -- ids)

    def addSubscriber(ref: ActorRef[AIControllerMessage.ObstacleGraph]): PathFinderInfo =
      copy(subscribers = subscribers + ref)

    def removeSubscriber(ref: ActorRef[Nothing]): PathFinderInfo =
      copy(subscribers = subscribers - ref.unsafeUpcast[AIControllerMessage.ObstacleGraph])

    lazy val graph: Graph = AIWanderGraph(shapeQT, entityRadius)._1

  }

  def apply(gameStateAtCreation: GameState): Behavior[Message] =
    receiver(PathFinderInfo(ShapeQT.empty ++ obstaclesFromGameState(gameStateAtCreation), Set()))

  private def receiver(pathFinderInfo: PathFinderInfo): Behavior[Message] = Behaviors.receive { (ctx, message) =>
    message match {
      case GameActionsWrapper(actions, gameState) =>
        val newObstacles       = actions.flatMap(maybeNewObstacle(_, gameState))
        val deletedObstacleIds = actions.flatMap(maybeRemoveObstacle)

        if (newObstacles.nonEmpty || deletedObstacleIds.nonEmpty) {
          ctx.self ! GraphUpdated
        }

        // We add before removing otherwise we could end up in an inconsistent state.
        receiver(pathFinderInfo.addObstacles(newObstacles).removeObstaclesByIds(deletedObstacleIds))
      case GraphUpdated =>
        println("Graph has been updated.")
        println(pathFinderInfo.graph.allEdges.length)
        println("*" * 60)
        pathFinderInfo.subscribers.foreach(_ ! AIControllerMessage.ObstacleGraph(pathFinderInfo.graph))
        Behaviors.same
      case NewSubscriber(ref) =>
        ref ! AIControllerMessage.ObstacleGraph(pathFinderInfo.graph)

        ctx.watchWith(ref, RemoveSubscriber(ref))

        receiver(pathFinderInfo.addSubscriber(ref))
      case RemoveSubscriber(ref) =>
        receiver(pathFinderInfo.removeSubscriber(ref))
    }
  }

}

object PathFinder {
  sealed trait Message

  /** Sent to this [[PathFinder]] for each new actions occuring in the game. */
  final case class GameActionsWrapper(actions: List[GameAction], currentGameState: GameState) extends Message

  /** Sent from this [[PathFinder]] to itself each time the graph is updated. */
  private case object GraphUpdated extends Message

  /** Sent to this [[PathFinder]] when an AI actor wants to be notified of the graph. */
  final case class NewSubscriber(ref: ActorRef[AIControllerMessage.ObstacleGraph]) extends Message

  /** Sent to this [[PathFinder]] when an AI actor wants to unsubscribe from graph updates.
    * This is done automatically when the `ref` actor dies.
    */
  final case class RemoveSubscriber(ref: ActorRef[Nothing]) extends Message

}
