package game

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.ActorMaterializer
import gamelogic.gamestate.gameactions.UpdateTimestamp
import gamelogic.gamestate.{ActionCollector, GameAction, GameState}
import io.circe.Encoder
import zio.ZIO
import zio.duration.Duration.fromScala

import scala.concurrent.duration._

object GameMaster {

  sealed trait Message

  case object GameLoop extends Message
  case class GameActionWrapper(gameAction: GameAction) extends Message

  private def now = System.currentTimeMillis

  private def gameLoopTo(to: ActorRef[GameLoop.type], delay: FiniteDuration) =
    for {
      fiber <- zio.clock.sleep(fromScala(delay)).fork
      _ <- fiber.join
      _ <- ZIO.effectTotal(to ! GameLoop)
    } yield ()

  def apply(
      pendingActions: List[GameAction],
      actionUpdateCollector: ActorRef[ActionUpdateCollector.ExternalMessage]
  ): Behavior[Message] = Behaviors.setup { implicit context =>
    //implicit val materializer: ActorMaterializer = ActorMaterializer()(context.system.toClassic)

    val actionCollector = new ActionCollector(GameState.initialGameState(now))

    def gameState = actionCollector.currentGameState

    Behaviors
      .receiveMessage[Message] {
        case GameActionWrapper(gameAction) =>
          // todo: We should check the minimal legality of actions here. That is, a position update of an entity
          // todo: should at least check that it is the given entity that sent the message.
          apply(
            gameAction +: pendingActions,
            actionUpdateCollector
          )
        case GameLoop =>
          val startTime     = now
          val sortedActions = (UpdateTimestamp(0L, startTime) +: pendingActions).sorted

          println(s"Time since last loop: ${startTime - gameState.time} ms")

          // Adding pending actions
          val (oldestToRemove, removedIds) = actionCollector.addAndRemoveActions(sortedActions)

          // Actual game logic (checking for dead things, collisions, and stuff)
          // todo

          // Sending new actions and removed illegal once
          actionUpdateCollector ! ActionUpdateCollector.AddAndRemoveActions(sortedActions, oldestToRemove, removedIds)

          val timeSpent = now - startTime

          if (timeSpent > gameLoopTiming) context.self ! GameLoop
          else //timerScheduler.startSingleTimer(GameLoop, (gameLoopTiming - timeSpent).millis)
            zio.Runtime.default.unsafeRunToFuture(
              gameLoopTo(context.self, (gameLoopTiming - timeSpent).millis)
            )

          apply(Nil, actionUpdateCollector)
      }

  }

  /** In millis */
  final val gameLoopTiming = 1000L / 120L

}
