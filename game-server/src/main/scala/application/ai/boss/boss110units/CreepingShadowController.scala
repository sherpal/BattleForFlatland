package application.ai.boss.boss110units

import application.ai.AIController
import application.ai.utils.maybeAbilityUsage
import gamelogic.abilities.Ability
import gamelogic.abilities.boss.boss110.addsabilities.CreepingShadowTick
import gamelogic.entities.Entity
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.boss.boss110.CreepingShadow
import gamelogic.entities.classes.{Constants, PlayerClass}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.gameactions.{EntityRadiusChange, MovingBodyMoves}
import gamelogic.gamestate.gameactions.boss110.AddCreepingShadow
import gamelogic.physics.Complex
import gamelogic.physics.pathfinding.Graph

/** The brain of the [[CreepingShadow]] of Boss110. It does not care about players: it follows the
  * small guys around (see [[CreepingShadow.computePositionAndRadius]]), grows with their number,
  * and ticks while it touches the boss.
  *
  * Registered in `AIManager` on the [[AddCreepingShadow]] action.
  */
class CreepingShadowController extends AIController[CreepingShadow, AddCreepingShadow] {

  override def loopRate: Int = 500

  /** The shadow's radius changes all the time, so there is no pathfinding graph for it. It flies
    * over everything anyway, so any graph will do.
    */
  override def computeActions(
      myId: Entity.Id,
      gameState: GameState,
      graphs: Double => Option[Graph]
  ): Vector[GameAction] =
    super.computeActions(myId, gameState, _ => graphs(Constants.playerRadius))

  override protected def takeActions(
      currentGameState: GameState,
      me: CreepingShadow,
      currentPosition: Complex,
      startTime: Long,
      timeSinceLastFrame: Long,
      maybeTarget: Option[PlayerClass],
      obstacleGraph: Graph
  ): Vector[GameAction] = {
    val (position, radius) = CreepingShadow.computePositionAndRadius(currentGameState, startTime)

    val maybeChangeRadius =
      Option.when(me.radius != radius)(
        EntityRadiusChange(GameAction.Id.dummy, startTime, me.id, radius)
      )

    val theoreticalDistance = me.speed * timeSinceLastFrame / 1000
    val distanceToTarget    = currentPosition.distanceTo(position)

    val maybeMove =
      if me.moving && distanceToTarget < theoreticalDistance then
        Some(
          MovingBodyMoves(
            GameAction.Id.dummy,
            startTime,
            me.id,
            position,
            Angle.zero,
            Angle.zero,
            me.speed,
            moving = false
          )
        )
      else if distanceToTarget > theoreticalDistance then
        Some(
          MovingBodyMoves(
            GameAction.Id.dummy,
            startTime,
            me.id,
            currentPosition,
            (position - currentPosition).arg,
            Angle.zero,
            me.speed,
            moving = true
          )
        )
      else None

    val maybeTick =
      maybeAbilityUsage(
        me,
        CreepingShadowTick(Ability.UseId.dummy, startTime, me.id),
        currentGameState
      ).map(_.toStartCasting(startTime))

    Vector(maybeMove, maybeChangeRadius, maybeTick).flatten
  }

  override protected def getMe(gameState: GameState, entityId: Entity.Id): Option[CreepingShadow] =
    CreepingShadow.extractCreepingShadow(gameState, entityId)

}
