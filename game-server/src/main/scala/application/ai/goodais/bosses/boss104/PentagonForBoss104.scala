package application.ai.goodais.bosses.boss104

import gamelogic.entities.Entity
import application.ai.goodais.classes.PentagonAIController
import gamelogic.abilities.Ability
import gamelogic.physics.Complex
import gamelogic.entities.classes.{Constants, Pentagon}
import gamelogic.physics.pathfinding.Graph
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.GameState
import gamelogic.entities.boss.boss104.DebuffCircle
import gamelogic.buffs.boss.boss104.TwinDebuff

case class PentagonForBoss104(index: Int, entityId: Entity.Id) extends PentagonAIController(index) {

  private val basePosition = Complex.polar(Constants.bossRadius * 3, index * math.Pi + math.Pi / 4)

  override protected def takeActions(
      currentGameState: GameState,
      me: Pentagon,
      currentPosition: Complex,
      startTime: Long,
      timeSinceLastFrame: Long,
      obstacleGraph: Graph
  ): Vector[GameAction] = handleTwinCircle(me, currentGameState, startTime).getOrElse {

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
                  circle.pos + Complex.polar(circle.shape.radius, index * math.Pi),
                  travelledDistance
                )
              )
            }
          }
      }
      .flatten
}
