package game.ai.goodais.boss.boss101

import game.ai.goodais.GoodAIController
import gamelogic.entities.classes.Triangle
import scala.reflect.ClassTag
import models.bff.outofgame.gameconfig.PlayerName
import gamelogic.gamestate.{GameAction, GameState}
import models.bff.outofgame.PlayerClasses
import gamelogic.entities.classes.Constants
import gamelogic.entities.boss.Boss101
import gamelogic.buffs.Buff
import game.ai.utils.maybeAbilityUsage
import gamelogic.abilities.WithTargetAbility
import gamelogic.abilities.triangle.UpgradeDirectHit
import gamelogic.abilities.triangle.DirectHit
import gamelogic.abilities.triangle.EnergyKick
import gamelogic.gamestate.gameactions.MovingBodyMoves

final class TriangleForBoss101(index: Int) extends GoodAIController[Triangle] {

  val classTag: ClassTag[Triangle] = implicitly[ClassTag[Triangle]]

  val name: PlayerName.AIPlayerName = PlayerName.AIPlayerName(PlayerClasses.Triangle, index)

  def loop(startingTime: Long, gameState: GameState, me: Triangle, sendActions: List[GameAction] => Unit): Unit = {
    import gamelogic.physics.Complex.DoubleWithI
    val previousPosition  = me.pos
    val currentPosition   = me.currentPosition(startingTime)
    val travelledDistance = previousPosition distanceTo currentPosition

    val actions: List[GameAction] = gameState.bosses.values.headOption match {
      case Some(theBoss) if theBoss.pos.distanceTo(currentPosition) < WithTargetAbility.meleeRange =>
        val shouldIBuffMyself =
          !gameState.allBuffsOfEntity(me.id).exists(_.resourceIdentifier == Buff.triangleUpgradeDirectHit)

        val maybeBuffMyself =
          Option
            .when(shouldIBuffMyself)(
              maybeAbilityUsage(me, UpgradeDirectHit(0L, startingTime, me.id), gameState).startCasting
            )
            .flatten

        val maybeDirectHit =
          Option
            .unless(shouldIBuffMyself)(
              maybeAbilityUsage(
                me,
                DirectHit(0L, startingTime, me.id, theBoss.id, DirectHit.directHitDamage),
                gameState
              ).startCasting
            )
            .flatten

        val maybeFiller = maybeAbilityUsage(me, EnergyKick(0L, startingTime, me.id, theBoss.id), gameState).startCasting

        List(
          Option.when(me.moving)(
            MovingBodyMoves(
              0L,
              startingTime,
              me.id,
              currentPosition,
              (theBoss.pos - me.pos).arg,
              (theBoss.pos - me.pos).arg,
              me.speed,
              moving = false
            )
          ),
          maybeFiller,
          maybeBuffMyself,
          maybeDirectHit
        ).flatten

      case Some(theBoss) =>
        val targetPosition = theBoss.pos + theBoss.shape.radius + me.shape.radius
        preGameMovement(startingTime, me, currentPosition, targetPosition, travelledDistance)
      case None =>
        val targetPosition = Boss101.bossStartingPosition + 60
        preGameMovement(startingTime, me, currentPosition, targetPosition, travelledDistance)
    }

    sendActions(actions)

  }

}
