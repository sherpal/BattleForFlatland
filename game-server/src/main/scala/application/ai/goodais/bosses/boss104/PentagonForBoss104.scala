package application.ai.goodais.bosses.boss104

import application.ai.goodais.classes.PentagonAIController
import gamelogic.abilities.Ability
import gamelogic.buffs.boss.boss104.TwinDebuff
import gamelogic.entities.Entity
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.boss.boss104.DebuffCircle
import gamelogic.entities.classes.{Constants, Pentagon}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.physics.pathfinding.Graph

/** Bot for the Pentagons of Boss104.
  *
  * Boss104's composition has two Pentagons, so `index` (0 or 1) is what tells them apart: it is
  * used below both to give each one its own resting spot around the boss, and to jitter their
  * reaction to the twin debuff circles so that they don't both rush to the same one.
  *
  * Outside of the twin debuff phase they just stand at their base position and shoot the boss.
  */
case class PentagonForBoss104(index: Int, entityId: Entity.Id)
    extends PentagonAIController(index)
    with DeathMarkTriangleAware[Pentagon] {

  private val basePosition = Complex.polar(
    Constants.bossRadius * 3,
    Angle.fromDouble(index.toDouble) * Angle.Pi + Angle.Pi / 4
  )

  override protected def takeActions(
      currentGameState: GameState,
      me: Pentagon,
      currentPosition: Complex,
      startTime: Long,
      timeSinceLastFrame: Long,
      obstacleGraph: Graph
  ): Vector[GameAction] = maybeHandleDeathMarkTriangle(currentGameState, me, startTime)
    .orElse(handleTwinCircle(me, currentGameState, startTime))
    .getOrElse {

      if currentPosition.distanceTo(basePosition) > 20 then {
        // go to base position
        val (previousPosition, currentPosition, travelledDistance) = someDistanceInfo(startTime, me)
        preGameMovement(startTime, me, currentPosition, basePosition, travelledDistance)
      } else {

        val maybeBoss = currentGameState.bosses.values.headOption

        def maybePentagonBullet = maybeBoss.flatMap(boss =>
          maybePentagonBulletUsage(
            currentGameState,
            startTime,
            me,
            (boss.currentPosition(startTime) - currentPosition).arg
          )
        )

        val maybeStopMoving = stopMoving(startTime, me, currentPosition, me.rotation)

        Vector(
          maybePentagonBullet,
          maybeStopMoving
        ).flatten
      }
    }

  def handleTwinCircle(me: Pentagon, gameState: GameState, time: Long): Option[Vector[GameAction]] =
    Option
      .unless(me.abilityOnCooldown(Ability.pentagonDispelId, time)) {
        val circles = gameState
          .allTEntities[DebuffCircle]
          .values
          .toVector
          .filter(time - _.time > index * 1000) // delay for index 1 to jitter a bit

        circles
          .minByOption(_.pos.distanceTo(me.currentPosition(time)))
          .flatMap { circle =>
            if me.collides(circle, time) then {
              // I am in the circle, I can debuff the player
              gameState.allTBuffs[TwinDebuff].find(_.colour == circle.colour).map(_.bearerId).map {
                target =>
                  maybeDispelUsage(gameState, time, target, me).toVector
              }
            } else {
              val (previousPosition, currentPosition, travelledDistance) =
                someDistanceInfo(time, me)
              Some(
                preGameMovement(
                  time,
                  me,
                  currentPosition,
                  // we jitter a bit the target position to reduce the chance that, in the beginning,
                  // both pentagon goes and arrive to the same circle at the same time
                  circle.pos + Complex
                    .polar(circle.shape.radius, Angle.fromDouble(index.toDouble) * Angle.Pi),
                  travelledDistance
                )
              )
            }
          }
      }
      .flatten
}
