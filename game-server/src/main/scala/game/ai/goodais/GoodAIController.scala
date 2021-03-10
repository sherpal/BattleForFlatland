package game.ai.goodais

import gamelogic.entities.Entity
import akka.actor.typed.ActorRef
import zio.ZIO
import gamelogic.abilities.Ability
import gamelogic.gamestate.gameactions.EntityStartsCasting
import gamelogic.gamestate.GameAction
import gamelogic.entities.MovingBody
import gamelogic.physics.Complex
import gamelogic.gamestate.gameactions.MovingBodyMoves
import scala.reflect.ClassTag
import akka.actor.typed.scaladsl.Behaviors
import gamelogic.gamestate.GameState
import akka.actor.typed.Behavior
import GoodAIController._
import models.bff.outofgame.gameconfig.PlayerName

import game.ai.goodais.boss.GoodAICreator.ActionTranslatorRef
import game.ActionTranslator
import zio.duration._
import gamelogic.buffs.Buff

trait GoodAIController[EntityType <: MovingBody] {

  val classTag: ClassTag[EntityType]

  private implicit def ct: ClassTag[EntityType] = classTag

  private def sendMeLoop(to: ActorRef[Loop.type], in: zio.duration.Duration) =
    for {
      fiber <- zio.clock.sleep(in).fork
      _     <- fiber.join
      _     <- ZIO.effectTotal(to ! Loop)
    } yield ()

  final def unsafeRunSendMeLoop(to: ActorRef[Loop.type], in: zio.duration.Duration): Unit =
    zio.Runtime.default.unsafeRunAsync(sendMeLoop(to, in))(_ => ())

  def now: Long = System.currentTimeMillis()

  def loopRate: Long = 1000L / 30L

  protected implicit class StartCasting(maybeAbility: Option[Ability]) {
    def startCasting: Option[EntityStartsCasting] = maybeAbility.map(
      ability => EntityStartsCasting(0L, ability.time, ability.castingTime, ability)
    )
  }

  protected implicit class IterableEnhanced[T](elements: Iterable[T]) {
    def avg(implicit fractional: Fractional[T]): T =
      if (elements.isEmpty) fractional.zero
      else {
        val sum    = elements.sum
        val length = fractional.fromInt(elements.size)
        fractional.div(sum, length)
      }
  }

  /**
    * Moves as a straight line to the target position.
    *
    * @param time current time for the movement action
    * @param me instance of myself
    * @param currentPosition position that I have currently
    * @param targetPosition position where I want to go
    * @param travelledDistance distance travelled since last time
    * @return list of [[GameAction]] (actually only one) to perform in order to go there
    */
  def preGameMovement(
      time: Long,
      me: EntityType,
      currentPosition: Complex,
      targetPosition: Complex,
      travelledDistance: Double
  ): List[GameAction] = {
    val distanceToTarget = me.pos distanceTo targetPosition

    if (travelledDistance > distanceToTarget || distanceToTarget < 3) {
      MovingBodyMoves(
        0L,
        time,
        me.id,
        targetPosition,
        me.direction,
        me.rotation,
        me.speed,
        moving = false
      ) :: Nil

    } else {
      val direction = (targetPosition - currentPosition).arg
      MovingBodyMoves(0L, time, me.id, currentPosition, direction, direction, me.speed, moving = true) :: Nil
    }

  }

  val name: PlayerName.AIPlayerName

  private case class ReceiverInfo(entityId: Entity.Id, gameState: GameState, actionTranslator: ActionTranslatorRef) {
    def withGameState(newGameState: GameState): ReceiverInfo = copy(gameState = newGameState)

    def maybeMe: Option[EntityType] = gameState.entityByIdAs[EntityType](entityId)

    def sendAction: List[GameAction] => Unit =
      (actions: List[GameAction]) => actionTranslator ! ActionTranslator.GameActionsWrapper(actions)
  }

  def apply(entityId: Entity.Id, actionTranslator: ActionTranslatorRef): Behavior[Command] =
    Behaviors.setup[Command] { context =>
      context.self ! Loop
      receiver(ReceiverInfo(entityId, GameState.empty, actionTranslator))
    }

  private def receiver(
      receiverInfo: ReceiverInfo
  ): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match {
        case GameStateWrapper(gameState) =>
          receiver(receiverInfo.withGameState(gameState))
        case Loop =>
          val startingTime = now
          receiverInfo.maybeMe.foreach(
            loop(startingTime, receiverInfo.gameState, _, receiverInfo.sendAction)
          )
          val timeTaken = now - startingTime
          unsafeRunSendMeLoop(context.self, ((loopRate - timeTaken) max 0L).millis)
          Behaviors.same
      }
    }

  def loop(
      startingTime: Long,
      gameState: GameState,
      me: EntityType,
      sendActions: List[GameAction] => Unit
  ): Unit

  /**
    * Returns a triplet containing information about the current and past positions of the entity
    *
    * @param time current time (of the loop)
    * @param me instance of the entity type this controller controls
    * @return the previous position of the AI (in last loop) and the current position (now)
    */
  final def someDistanceInfo(time: Long, me: EntityType): (Complex, Complex) = {
    val previousPosition = me.pos
    val currentPosition  = me.currentPosition(time)

    (previousPosition, currentPosition)
  }

  final def buffsOnMe(gameState: GameState, me: EntityType): List[Buff] =
    gameState.allBuffsOfEntity(me.id).toList

}

object GoodAIController {
  sealed trait Command

  case class GameStateWrapper(gameState: GameState) extends Command
  case object Loop extends Command
}
