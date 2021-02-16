package game.ai.boss.boss110units

import game.ai.boss.AIController
import game.ai.utils._
import gamelogic.entities.Entity.Id
import gamelogic.entities.boss.boss110.BigGuy
import gamelogic.entities.classes.PlayerClass
import gamelogic.gamestate.gameactions.EntityStartsCasting
import gamelogic.gamestate.gameactions.boss110.AddBigGuies
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.physics.pathfinding.Graph
import gamelogic.abilities.boss.boss110.addsabilities.PutBrokenArmor

object BigGuyController extends AIController[BigGuy, AddBigGuies.AddBigGuy] {
  protected def takeActions(
      currentGameState: GameState,
      me: BigGuy,
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
      BigGuy.range,
      BigGuy.fullSpeed,
      BigGuy.fullSpeed / 10,
      me.speed,
      me.moving,
      me.rotation
    )

    val maybeAttack =
      me.maybeAutoAttack(startTime)
        .map(ability => EntityStartsCasting(0L, startTime, ability.castingTime, ability))
        .filter(_.isLegalBoolean(currentGameState))

    val maybePutBrokenArmor =
      Some(PutBrokenArmor(0L, startTime, me.id, target.id))
        .filter(me.canUseAbilityBoolean(_, startTime))
        .map(ability => EntityStartsCasting(0L, startTime, ability.castingTime, ability))
        .filter(_.isLegal(currentGameState).isEmpty)

    useAbility(
      List(
        maybePutBrokenArmor,
        maybeAttack
      ),
      maybeChangeTarget,
      maybeMove
    )
  }

  protected def getMe(gameState: GameState, entityId: Id): Option[BigGuy] =
    gameState.entityById(entityId).collect { case bigGuy: BigGuy => bigGuy }
}
