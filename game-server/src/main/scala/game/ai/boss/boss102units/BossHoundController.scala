package game.ai.boss.boss102units

import game.ai.boss.AIController
import game.ai.utils.{aiMovementToTarget, changeTarget}
import gamelogic.entities.Entity.Id
import gamelogic.entities.boss.boss102.BossHound
import gamelogic.entities.classes.PlayerClass
import gamelogic.gamestate.gameactions.EntityStartsCasting
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.gameactions.boss102.AddBossHound
import gamelogic.physics.Complex

object BossHoundController extends AIController[BossHound, AddBossHound] {
  protected def takeActions(
      currentGameState: GameState,
      me: BossHound,
      currentPosition: Complex,
      startTime: Long,
      maybeTarget: Option[PlayerClass]
  ): List[GameAction] = maybeTarget.fold(List[GameAction]()) { target =>
    /** changing target */
    val maybeChangeTarget = changeTarget(me, target.id, startTime)

    val maybeMove = aiMovementToTarget(
      me.id,
      startTime,
      currentPosition,
      me.shape.radius,
      target.currentPosition(startTime),
      BossHound.range,
      BossHound.fullSpeed,
      BossHound.fullSpeed,
      me.moving,
      me.rotation
    )

    val maybeAttack =
      me.maybeAutoAttack(startTime).map(ability => EntityStartsCasting(0L, startTime, ability.castingTime, ability))

    List(maybeChangeTarget, maybeMove, maybeAttack).flatten
  }

  protected def getMe(gameState: GameState, entityId: Id): Option[BossHound] =
    gameState.entityById(entityId).collect { case hound: BossHound => hound }
}
