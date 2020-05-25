package game.ai.boss

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import game.ActionTranslator
import game.ai.AIControllerMessage
import game.ai.AIManager.loopRate
import gamelogic.abilities.Ability
import gamelogic.abilities.boss.boss101.{BigDot, BigHit}
import gamelogic.entities.boss.Boss101
import gamelogic.gamestate.GameState
import gamelogic.gamestate.gameactions.{ChangeTarget, EntityStartsCasting, MovingBodyMoves, SpawnBoss}

import scala.concurrent.duration._
import scala.util.Random

object Boss101Controller {

  @inline private def now = System.currentTimeMillis

  def apply(
      actionTranslator: ActorRef[ActionTranslator.Message],
      initialMessage: SpawnBoss
  ): Behavior[AIControllerMessage] = Behaviors.setup { context =>
    Behaviors.receiveMessage {
      case AIControllerMessage.GameStateWrapper(gameState) =>
        AIControllerMessage.unsafeRunSendMeLoop(context.self, zio.duration.Duration.fromScala(loopRate.millis))
        receiver(actionTranslator, spawnBoss = initialMessage, currentGameState = gameState)
      case _ =>
        //waiting for first game state
        Behaviors.same
    }
  }

  private def receiver(
      actionTranslator: ActorRef[ActionTranslator.Message],
      spawnBoss: SpawnBoss,
      currentGameState: GameState
  ): Behavior[AIControllerMessage] = Behaviors.receive { (context, message) =>
    def myId = spawnBoss.entityId

    message match {
      case AIControllerMessage.GameStateWrapper(gameState) =>
        gameState.bosses
          .get(myId)
          .fold(Behaviors.stopped[AIControllerMessage])(
            _ =>
              receiver(
                actionTranslator,
                spawnBoss,
                gameState
              )
          )
      case AIControllerMessage.NewActions(_) => Behaviors.same
      case AIControllerMessage.Loop =>
        val startTime       = now
        val me              = currentGameState.bosses(myId)
        val currentPosition = me.currentPosition(startTime)

        val maybeTarget = me.damageThreats
          .maxByOption(_._2)
          .map(_._1)
          .flatMap(
            currentGameState.players.get // this could change in the future
          )
          .fold(currentGameState.players.headOption.map(_._2))(Some(_))

        Option.unless(currentGameState.entityIsCasting(myId))(maybeTarget).flatten.foreach { target =>
          // If the boss is casting, he doesn't do anything else.
          // If the boss has no target, the only possibility is that all players are dead.
          // In that case, the game either has not started yet or it will end very soon so we don't do anything.

          /** changing target */
          val maybeChangeTarget = Option.unless(target.id == me.targetId)(ChangeTarget(0L, now, me.id, target.id))

          val bigHit = BigHit(0L, now, myId, target.id)

          val maybeUseAnAbility = Option
            .when(me.canUseAbility(bigHit, startTime)) {
              println("Using BigHit!")
              EntityStartsCasting(0L, now, bigHit.castingTime, bigHit)
            }
            .orElse(
              Some(
                BigDot(
                  0L,
                  now,
                  me.id,
                  // targeting someone at random besides the target
                  currentGameState.players
                    .filterNot(_._1 == target.id)
                    .keys
                    .maxByOption(_ => Random.nextInt())
                    .getOrElse(target.id)
                )
              ).filter(ability => me.canUseAbility(ability, startTime)).map { ability =>
                EntityStartsCasting(0L, now, ability.castingTime, ability)
              }
            )

          /** Move to target */
          val targetPosition  = target.currentPosition(startTime)
          val directionVector = targetPosition - currentPosition
          val maybeBodyMove =
            Option
              .unless(maybeUseAnAbility.exists(_.ability.abilityId == Ability.boss101BigHitId))(
                MovingBodyMoves(
                  0L,
                  startTime,
                  myId,
                  currentPosition,
                  directionVector.arg,
                  directionVector.arg,
                  me.speed,
                  directionVector.modulus > Boss101.meleeRange
                )
              )
              .filter(
                action =>
                  action.moving || action.moving != me.moving || (action.position - me.pos).modulus > 1e-6 || (directionVector.arg != me.rotation)
              )

          List(
            maybeChangeTarget,
            maybeBodyMove,
            maybeUseAnAbility
          ).flatten match {
            case Nil     =>
            case actions => actionTranslator ! ActionTranslator.GameActionsWrapper(actions)
          }

        }

        val timeTaken = now - startTime
        AIControllerMessage.unsafeRunSendMeLoop(
          context.self,
          zio.duration.Duration.fromScala(
            ((loopRate - timeTaken) max 0).millis
          )
        )

        Behaviors.same
    }
  }

}
