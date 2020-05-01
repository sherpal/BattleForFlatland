package game

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.{AddPlayer, GameStart, UpdateTimestamp}
import gamelogic.gamestate.{ActionCollector, GameAction, GameState}
import gamelogic.physics.Complex
import models.bff.ingame.InGameWSProtocol
import models.bff.outofgame.MenuGameWithPlayers
import zio.ZIO
import zio.duration.Duration.fromScala

import scala.concurrent.duration._
import scala.util.{Failure, Random, Success}

object GameMaster {

  sealed trait Message

  sealed trait InGameMessage extends Message

  /** Sent by this actor to itself to run the game loop. We try to keep a rate of 120 per second. */
  private case object GameLoop extends InGameMessage

  /** A single action was sent by a game entity. We add it to the queue to be processed during the game loop */
  case class GameActionWrapper(gameAction: GameAction) extends InGameMessage

  /** Same as [[GameActionWrapper]] but for multiple actions. */
  case class MultipleActionsWrapper(gameActions: List[GameAction]) extends InGameMessage

  sealed trait PreGameMessage extends Message

  /**
    * Sent by the [[game.AntiChamber]] when every client is ready.
    * The map is used so that the game master can tell the client what entity id they have at the beginning of the game.
    * @param playerMap map from the userId to the client actor
    * @param gameInfo information of the game, in order to know what kind of game to create, and what to put in it.
    */
  case class EveryoneIsReady(playerMap: Map[String, ActorRef[InGameWSProtocol]], gameInfo: MenuGameWithPlayers)
      extends PreGameMessage

  private def now = System.currentTimeMillis

  private def gameLoopTo(to: ActorRef[GameLoop.type], delay: FiniteDuration) =
    for {
      fiber <- zio.clock.sleep(fromScala(delay)).fork
      _ <- fiber.join
      _ <- ZIO.effectTotal(to ! GameLoop)
    } yield ()

  def apply(actionUpdateCollector: ActorRef[ActionUpdateCollector.ExternalMessage]): Behavior[Message] =
    setupBehaviour(actionUpdateCollector)

  def inGameBehaviour(
      pendingActions: List[GameAction],
      actionUpdateCollector: ActorRef[ActionUpdateCollector.ExternalMessage],
      lastActionId: GameAction.Id,
      lastEntityId: Entity.Id
  ): Behavior[Message] = Behaviors.setup { implicit context =>
    // todo: fix this -1000
    val actionCollector = new ActionCollector(GameState.initialGameState(now - 1000))

    def gameState = actionCollector.currentGameState

    Behaviors
      .receiveMessage[Message] {
        case GameActionWrapper(gameAction) =>
          // todo: We should check the minimal legality of actions here. That is, a position update of an entity
          // todo: should at least check that it is the given entity that sent the message.

          // Note: we sort actions during the game loop so we can simply prepend and enjoy the O(1) complexity.
          inGameBehaviour(
            gameAction +: pendingActions,
            actionUpdateCollector,
            lastActionId,
            lastEntityId
          )
        case MultipleActionsWrapper(gameActions) =>
          inGameBehaviour(gameActions ++ pendingActions, actionUpdateCollector, lastActionId, lastEntityId)
        case GameLoop =>
          // this is quite ugly. Can I do better?
          var _lastGameActionId = lastActionId
          def nextGameActionId(): GameAction.Id = {
            _lastGameActionId += 1
            _lastGameActionId
          }

          var _lastEntityId = lastEntityId
          def nextEntityId(): Entity.Id = {
            _lastEntityId += 1
            _lastGameActionId
          }

          val startTime = now
          val sortedActions = pendingActions.sorted
            .map(_.changeId(nextGameActionId()))

          //println(s"Time since last loop: ${startTime - gameState.time} ms")

          // Adding pending actions
          try {
            val (oldestToRemove, removedIds) = actionCollector.addAndRemoveActions(sortedActions)

            // Actual game logic (checking for dead things, collisions, and stuff)
            // todo

            // Sending new actions and removed illegal once
            if (sortedActions.nonEmpty || removedIds.nonEmpty) {
              actionUpdateCollector ! ActionUpdateCollector
                .AddAndRemoveActions(sortedActions, oldestToRemove, removedIds)
            }

            val timeSpent = now - startTime

            if (timeSpent > gameLoopTiming) context.self ! GameLoop
            else
              zio.Runtime.default
                .unsafeRunToFuture(
                  gameLoopTo(context.self, (gameLoopTiming - timeSpent).millis)
                )
          } catch {
            case e: Throwable =>
              e.printStackTrace()
          }

          inGameBehaviour(Nil, actionUpdateCollector, _lastGameActionId, _lastEntityId)

        case _: PreGameMessage => Behaviors.unhandled
      }

  }

  /** In millis */
  final val gameLoopTiming = 1000L / 120L

  def setupBehaviour(actionUpdateCollector: ActorRef[ActionUpdateCollector.ExternalMessage]): Behavior[Message] =
    Behaviors.receive { (context, message) =>
      message match {
        case message: PreGameMessage =>
          message match {
            case EveryoneIsReady(playerMap, _) =>
              val n = now
              context.self ! GameActionWrapper(GameStart(0L, n))

              val newPlayerActions = playerMap.values.zipWithIndex.map {
                case (ref, idx) =>
                  ref -> AddPlayer(
                    0,
                    n - 1,
                    idx.toLong,
                    100 * Complex.rotation(idx * 2 * math.Pi / playerMap.size),
                    Random.nextInt(0xFFFFFF)
                  )
              }

              newPlayerActions.foreach {
                case (ref, player) =>
                  ref ! InGameWSProtocol.YourEntityIdIs(player.playerId)
              }

              context.self ! MultipleActionsWrapper(newPlayerActions.map(_._2).toList)

              context.self ! GameLoop
              inGameBehaviour(Nil, actionUpdateCollector, 0L, playerMap.size - 1)
          }

        case _ =>
          Behaviors.unhandled
      }
    }

}
