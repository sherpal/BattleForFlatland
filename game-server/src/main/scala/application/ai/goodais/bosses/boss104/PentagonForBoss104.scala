package application.ai.goodais.bosses.boss104

import gamelogic.entities.Entity
import application.ai.goodais.classes.PentagonAIController
import gamelogic.physics.Complex
import gamelogic.entities.classes.Pentagon
import gamelogic.physics.pathfinding.Graph
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.GameState
import gamelogic.entities.boss.boss104.DebuffCircle
import gamelogic.buffs.boss.boss104.TwinDebuff

case class PentagonForBoss104(index: Int, entityId: Entity.Id) extends PentagonAIController(index) {

  val defaultPosition = Complex.polar(70, math.Pi * index)

  override protected def takeActions(
      currentGameState: GameState,
      me: Pentagon,
      currentPosition: Complex,
      startTime: Long,
      timeSinceLastFrame: Long,
      obstacleGraph: Graph
  ): Vector[GameAction] = handleTwinCircle(me, currentGameState, startTime).getOrElse {

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

  def handleTwinCircle(me: Pentagon, gameState: GameState, time: Long): Option[Vector[GameAction]] =
    val circles = gameState
      .allTEntities[DebuffCircle]
      .values
      .toVector
      .sortBy(circle => (circle.pos.modulus2, circle.pos.re, circle.pos.im))
    Option
      .when(circles.length > index) {
        val circle = circles(index)
        if me.collides(circle, time) then {
          // I am in the circle, I can debuff the player
          gameState.allTBuffs[TwinDebuff].find(_.colour == circle.colour).map(_.bearerId).map {
            target =>
              maybeDispelUsage(gameState, time, target, me).toVector
          }
        } else {
          val (previousPosition, currentPosition, travelledDistance) =
            someDistanceInfo(time, me)
          Some(preGameMovement(time, me, currentPosition, circle.pos, travelledDistance))
        }
      }
      .flatten
}
