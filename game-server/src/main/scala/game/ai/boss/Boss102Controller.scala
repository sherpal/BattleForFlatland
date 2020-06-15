package game.ai.boss
import game.ai.utils.{aiMovementToTarget, changeTarget}
import gamelogic.abilities.AutoAttack
import gamelogic.abilities.boss.boss102.{PutDamageZones, SpawnHound}
import gamelogic.entities.Entity.Id
import gamelogic.entities.boss.BossEntity
import gamelogic.entities.boss.dawnoftime.Boss102
import gamelogic.entities.classes.PlayerClass
import gamelogic.gamestate.gameactions.{EntityStartsCasting, SpawnBoss}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex

object Boss102Controller extends AIController[Boss102, SpawnBoss] {
  protected def takeActions(
      currentGameState: GameState,
      me: Boss102,
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
            action =>
              List(
                maybeChangeTarget,
                maybeMove.map(_.copy(moving = false)),
                maybeAction
              ).flatten
          )

        val maybeUseAbility = maybePutDamageZones
          .orElse(maybeSpawnHound)
          .orElse(
            me.maybeAutoAttack(startTime, currentGameState)
              .map(ability => EntityStartsCasting(0L, startTime, ability.castingTime, ability))
          )

        useAbility(maybeUseAbility)
      }
      .getOrElse(Nil)

  protected def getMe(gameState: GameState, entityId: Id): Option[Boss102] =
    gameState.bosses.get(entityId).collect { case boss102: Boss102 => boss102 }
}
