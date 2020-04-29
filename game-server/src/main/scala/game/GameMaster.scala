package game

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import gamelogic.gamestate.gameactions.UpdateTimestamp
import gamelogic.gamestate.{ActionCollector, GameAction, GameState}
import models.bff.ingame.InGameWSProtocol
import models.bff.outofgame.MenuGameWithPlayers
import zio.ZIO
import zio.duration.Duration.fromScala

import scala.concurrent.duration._

object GameMaster {

  sealed trait Message

  sealed trait InGameMessage extends Message
  case object GameLoop extends InGameMessage
  case class GameActionWrapper(gameAction: GameAction) extends InGameMessage
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
      actionUpdateCollector: ActorRef[ActionUpdateCollector.ExternalMessage]
  ): Behavior[Message] = Behaviors.setup { implicit context =>
    val actionCollector = new ActionCollector(GameState.initialGameState(now))

    def gameState = actionCollector.currentGameState

    Behaviors
      .receiveMessage[Message] {
        case GameActionWrapper(gameAction) =>
          // todo: We should check the minimal legality of actions here. That is, a position update of an entity
          // todo: should at least check that it is the given entity that sent the message.
          inGameBehaviour(
            gameAction +: pendingActions,
            actionUpdateCollector
          )
        case MultipleActionsWrapper(gameActions) =>
          inGameBehaviour(gameActions ++ pendingActions, actionUpdateCollector)
        case GameLoop =>
          val startTime     = now
          val sortedActions = (UpdateTimestamp(0L, startTime) +: pendingActions).sorted

          //println(s"Time since last loop: ${startTime - gameState.time} ms")

          // Adding pending actions
          val (oldestToRemove, removedIds) = actionCollector.addAndRemoveActions(sortedActions)

          // Actual game logic (checking for dead things, collisions, and stuff)
          // todo

          // Sending new actions and removed illegal once
          actionUpdateCollector ! ActionUpdateCollector.AddAndRemoveActions(sortedActions, oldestToRemove, removedIds)

          val timeSpent = now - startTime

          if (timeSpent > gameLoopTiming) context.self ! GameLoop
          else
            zio.Runtime.default.unsafeRunToFuture(
              gameLoopTo(context.self, (gameLoopTiming - timeSpent).millis)
            )

          inGameBehaviour(Nil, actionUpdateCollector)

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
              playerMap.values.zipWithIndex.foreach {
                case (ref, idx) => ref ! InGameWSProtocol.YourEntityIdIs(idx.toLong) // just to test
              }

              context.self ! GameLoop
              inGameBehaviour(Nil, actionUpdateCollector)
          }

        case _ =>
          Behaviors.unhandled
      }
    }

}
