package game.ai.boss.boss102units

import game.ai.boss.AIController
import game.ai.utils._
import gamelogic.entities.Entity.Id
import gamelogic.entities.boss.boss102.BossHound
import gamelogic.entities.classes.PlayerClass
import gamelogic.gamestate.gameactions.EntityStartsCasting
import gamelogic.gamestate.gameactions.boss102.AddBossHound
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.physics.pathfinding.Graph

object BossHoundController extends AIController[BossHound, AddBossHound] {
  protected def takeActions(
      currentGameState: GameState,
      me: BossHound,
      currentPosition: Complex,
      startTime: Long,
      lastTimeStamp: Long,
      maybeTarget: Option[PlayerClass],
      obstacleGraph: Graph
  ): List[GameAction] = maybeTarget.fold(List[GameAction]()) { target =>
    /** changing target */
    val maybeChangeTarget = changeTarget(me, target.id, startTime)

    val maybeMove = aiMovementToTarget(
      me.id,
      startTime,
      lastTimeStamp,
      currentPosition,
      me.shape.radius,
      target.currentPosition(startTime),
      BossHound.range,
      BossHound.fullSpeed,
      BossHound.fullSpeed / 10,
      me.speed,
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
