package game.ai.boss
import game.ai.utils.{aiMovementToTarget, changeTarget}
import gamelogic.abilities.boss.boss102.{PutDamageZones, SpawnHound}
import gamelogic.entities.Entity.Id
import gamelogic.entities.boss.BossEntity
import gamelogic.entities.boss.dawnoftime.Boss102
import gamelogic.entities.classes.PlayerClass
import gamelogic.gamestate.gameactions.{EntityStartsCasting, SpawnBoss}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex

object Boss102Controller extends AIController[BossEntity, SpawnBoss] {
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

        /** changing target */
        val maybeChangeTarget = changeTarget(me, target.id, startTime)

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

        val maybeSpawnHound =
          Some(SpawnHound(0L, startTime, me.id, Complex.zero))
            .filter(me.canUseAbility(_, startTime))
            .map(ability => EntityStartsCasting(0L, startTime, ability.castingTime, ability))

        def useAbility(maybeAction: Option[EntityStartsCasting]): List[GameAction] =
          maybeAction.fold(
            List(maybeChangeTarget, maybeMove).flatten
          )(
            _ =>
              List(
                maybeChangeTarget,
                maybeMove.map(_.copy(moving = false)),
                maybeAction
              ).flatten
          )

        val maybeUseAbility = maybePutDamageZones.orElse(maybeSpawnHound)

        useAbility(maybeUseAbility)
      }
      .getOrElse(Nil)

  protected def getMe(gameState: GameState, entityId: Id): Option[BossEntity] = gameState.bosses.get(entityId)
}
