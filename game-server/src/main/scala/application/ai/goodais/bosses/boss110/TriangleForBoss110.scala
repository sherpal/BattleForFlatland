package application.ai.goodais.bosses.boss110

import application.ai.goodais.classes.TriangleAIController
import gamelogic.abilities.WithTargetAbility
import gamelogic.abilities.triangle.Stun
import gamelogic.entities.{Entity, LivingEntity, MovingBody}
import gamelogic.entities.boss.dawnoftime.Boss110
import gamelogic.entities.classes.Triangle
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex

/** The two Triangles of Boss110 deal with the big guys. Each of them keeps one big guy stunned (so
  * that only one at a time breaks the armour of the tank), and both kill the remaining one. With no
  * big guy around, they hit the boss.
  *
  * They always keep enough energy to stun.
  */
final case class TriangleForBoss110(index: Int, entityId: Entity.Id)
    extends TriangleAIController(index)
    with Boss110Bot[Triangle] {

  private def maybeTarget(gameState: GameState): Option[LivingEntity & MovingBody] =
    Boss110Bot.bigGuyToKill(gameState).orElse(gameState.bosses.values.headOption)

  def restingPosition(gameState: GameState, me: Triangle, time: Long): (Complex, Double) =
    maybeTarget(gameState) match {
      case Some(target) => target.currentPosition(time) -> (WithTargetAbility.meleeRange - 10)
      case None => Complex(Boss110.halfWidth / 2, (index - 0.5) * Boss110.halfHeight) -> 10.0
    }

  override def instantActions(
      gameState: GameState,
      me: Triangle,
      time: Long
  ): Vector[GameAction] = {
    val maybeStun = Boss110Bot
      .bigGuyToStun(gameState, index)
      .filterNot(bigGuy => Boss110Bot.isStunnedBy(gameState, bigGuy.id, me.id))
      .flatMap(maybeStunUsage(gameState, time, me, _))

    def maybeAttack = maybeTarget(gameState).flatMap { target =>
      defaultAggressiveAbility(gameState, me, time, target)
        .filter(cast => me.resourceAmount.amount >= Stun.cost.amount + cast.ability.cost.amount)
        .orElse(maybeEnergyKickUsage(gameState, time, me, target))
    }

    maybeStun.orElse(maybeAttack).toVector
  }

  def actionsInPlace(gameState: GameState, me: Triangle, time: Long): Vector[GameAction] =
    Vector.empty

}
