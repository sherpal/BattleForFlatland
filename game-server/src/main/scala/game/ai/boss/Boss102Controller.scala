package game.ai.boss
import game.ai.utils.aiMovementToTarget
import gamelogic.abilities.boss.boss102.PutDamageZones
import gamelogic.entities.boss.BossEntity
import gamelogic.entities.boss.dawnoftime.Boss102
import gamelogic.entities.classes.PlayerClass
import gamelogic.gamestate.gameactions.EntityStartsCasting
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex

object Boss102Controller extends BossController {
  protected def takeActions(
      currentGameState: GameState,
      me: BossEntity,
      currentPosition: Complex,
      startTime: Long,
      maybeTarget: Option[PlayerClass]
  ): List[GameAction] =
    Option
      .unless(currentGameState.entityIsCasting(me.id))(maybeTarget)
      .flatten
      .map { target =>
        // If the boss is casting, he doesn't do anything else.
        // If the boss has no target, the only possibility is that all players are dead.
        // In that case, the game either has not started yet or it will end very soon so we don't do anything.
        val maybeMove = aiMovementToTarget(
          me.id,
          startTime,
          currentPosition,
          me.shape.radius,
          target.currentPosition(startTime),
          Boss102.meleeRange,
          Boss102.fullSpeed,
          Boss102.fullSpeed / 4,
          me.moving,
          me.rotation
        )

        val maybePutDamageZones =
          Some(PutDamageZones(0L, startTime, me.id, currentGameState.players.valuesIterator.toList.map(_.id)))
            .filter(me.canUseAbility(_, startTime))
            .map(ability => EntityStartsCasting(0L, startTime, ability.castingTime, ability))

        maybePutDamageZones match {
          case Some(action) =>
            maybeMove.map(_.copy(moving = false)).toList :+ action
          case None =>
            maybeMove.toList
        }
      }
      .getOrElse(Nil)
}
