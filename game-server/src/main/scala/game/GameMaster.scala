package game

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.ActorMaterializer
import gamelogic.gamestate.gameactions.UpdateTimestamp
import gamelogic.gamestate.{GameAction, GameState}
import io.circe.Encoder
import zio.ZIO
import zio.duration.Duration.fromScala

import scala.concurrent.duration._

object GameMaster {

  sealed trait Message

  case object GameLoop extends Message
  case class GameActionWrapper(gameAction: GameAction) extends Message

  private def gameLoopTo(to: ActorRef[GameLoop.type], delay: FiniteDuration) =
    for {
      fiber <- zio.clock.sleep(fromScala(delay)).fork
      _ <- fiber.join
      _ <- ZIO.effectTotal(to ! GameLoop)
    } yield ()

  def apply(gameState: GameState, pendingActions: List[GameAction]): Behavior[Message] = Behaviors.setup {
    implicit context =>
      implicit val materializer: ActorMaterializer = ActorMaterializer()(context.system.toClassic)

      Behaviors
        .receiveMessage[Message] {
          case GameActionWrapper(gameAction) =>
            apply(
              gameState,
              gameAction +: pendingActions
            )
          case GameLoop =>
            val startTime     = System.currentTimeMillis
            val sortedActions = (UpdateTimestamp(0L, startTime) +: pendingActions).sorted

            println(s"Time since last loop: ${startTime - gameState.time} ms")

            // todo: verify actions legality and stuff, tell people that stuff is going on.
            val newGameState = gameState.applyActions(sortedActions)

            val timeSpent = System.currentTimeMillis - startTime

            if (timeSpent > gameLoopTiming) context.self ! GameLoop
            else //timerScheduler.startSingleTimer(GameLoop, (gameLoopTiming - timeSpent).millis)
              zio.Runtime.default.unsafeRunToFuture(
                gameLoopTo(context.self, (gameLoopTiming - timeSpent).millis)
              )

            apply(newGameState, Nil)
        }

  }

  /** In millis */
  final val gameLoopTiming = 1000L / 120L

}
