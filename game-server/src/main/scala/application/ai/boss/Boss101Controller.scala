package application.ai.boss

import gamelogic.entities.Entity
import application.ai.AIController
import gamelogic.entities.boss.Boss101
import gamelogic.gamestate.gameactions.*
import gamelogic.physics.Complex
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.GameState
import gamelogic.physics.pathfinding.Graph
import gamelogic.entities.classes.PlayerClass
import gamelogic.entities.Entity.Id
import application.ai.utils.*
import gamelogic.abilities.boss.boss101.BigHit
import gamelogic.abilities.boss.boss101.BigDot
import scala.util.Random
import gamelogic.abilities.boss.boss101.SmallHit
import gamelogic.abilities.Ability

class Boss101Controller extends AIController[Boss101, SpawnBoss] {

  override protected def takeActions(
      currentGameState: GameState,
      me: Boss101,
      currentPosition: Complex,
      startTime: Long,
      timeSinceLastFrame: Long,
      maybeTarget: Option[PlayerClass],
      obstacleGraph: Graph
  ): Vector[GameAction] =
    Option.unless(currentGameState.entityIsCasting(me.id))(maybeTarget).flatten.toVector.flatMap {
      target =>
        println("Boss101 taking decisions!")

        // If the boss is casting, he doesn't do anything else.
        // If the boss has no target, the only possibility is that all players are dead.
        // In that case, the game either has not started yet or it will end very soon so we don't do anything.
        val myId = me.id

        /** changing target */
        val maybeChangeTarget = changeTarget(me, target.id, startTime)

        val bigHit = BigHit(Ability.UseId.zero, startTime, myId, target.id)

        val maybeUseAnAbility =
          Option
            .when(
              me.canUseAbilityBoolean(bigHit, startTime) && bigHit
                .isInRangeAndInSight(currentGameState, startTime)
                .isEmpty
            ) {
              Vector(
                MovingBodyMoves(
                  GameAction.Id.zero,
                  startTime,
                  myId,
                  currentPosition,
                  me.direction,
                  me.rotation,
                  me.speed,
                  moving = false
                ),
                EntityStartsCasting(GameAction.Id.zero, startTime, bigHit.castingTime, bigHit)
              )

            }
            .orElse(
              Some(
                BigDot(
                  Ability.UseId.zero,
                  startTime,
                  me.id,
                  // targeting someone at random besides the target
                  currentGameState.players
                    .filterNot(_._1 == target.id)
                    .keys
                    .maxByOption(_ => Random.nextInt())
                    .getOrElse(target.id)
                )
              ).filter(ability => me.canUseAbilityBoolean(ability, startTime)).map { ability =>
                List(
                  EntityStartsCasting(GameAction.Id.zero, startTime, ability.castingTime, ability)
                )
              }
            )
            .orElse(
              Some(
                SmallHit(Ability.UseId.zero, startTime, me.id, target.id, SmallHit.damageAmount)
              ).filter(ability => me.canUseAbilityBoolean(ability, startTime)).map { ability =>
                List(
                  EntityStartsCasting(GameAction.Id.zero, startTime, ability.castingTime, ability)
                )
              }
            )

        val shouldINotMove = maybeUseAnAbility.fold(false) { actions =>
          actions
            .collect { case a: EntityStartsCasting => a }
            .exists(_.ability.abilityId == Ability.boss101BigHitId)
        }

        /** Move to target */
        val targetPosition  = target.currentPosition(startTime)
        val directionVector = targetPosition - currentPosition
        val maybeBodyMove =
          Option
            .unless(shouldINotMove)(
              aiMovementToTarget(
                me.id,
                startTime,
                timeSinceLastFrame,
                currentPosition,
                me.shape.radius,
                targetPosition,
                Boss101.meleeRange,
                Boss101.fullSpeed,
                Boss101.fullSpeed / 4,
                me.speed,
                me.moving,
                me.rotation
              )
            )
            .flatten
            .filter(action =>
              action.moving || action.moving != me.moving || (action.position - me.pos).modulus > 1e-6 || (directionVector.arg != me.rotation)
            )

        maybeUseAnAbility.getOrElse(Vector.empty) ++
          Vector(
            maybeChangeTarget,
            maybeBodyMove
          ).flatten
    }

  override protected def getMe(gameState: GameState, entityId: Id): Option[Boss101] =
    gameState.bosses.get(entityId).collect { case boss101: Boss101 =>
      boss101
    }

}
