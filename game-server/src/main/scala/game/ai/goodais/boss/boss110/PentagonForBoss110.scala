package game.ai.goodais.boss.boss110

import game.ai.goodais.classes.PentagonAIController
import models.bff.outofgame.gameconfig.PlayerName
import gamelogic.entities.classes.Pentagon
import gamelogic.gamestate.{GameAction, GameState}
import models.bff.outofgame.PlayerClasses
import gamelogic.entities.boss.dawnoftime.Boss110
import gamelogic.physics.Complex
import gamelogic.entities.boss.boss110.CreepingShadow
import gamelogic.entities.boss.boss110.SmallGuy
import gamelogic.entities.boss.boss110.BombPod

final class PentagonForBoss110(index: Int) extends PentagonAIController {

  val name: PlayerName.AIPlayerName = PlayerName.AIPlayerName(PlayerClasses.Pentagon, index)

  def loop(startingTime: Long, gameState: GameState, me: Pentagon, sendActions: List[GameAction] => Unit): Unit = {
    val (previousPosition, currentPosition) = someDistanceInfo(startingTime, me)
    val travelledDistance                   = previousPosition distanceTo currentPosition

    val bombPods                = gameState.allTEntities[BombPod].values.toList
    val maybeDestinationBombPod = bombPods.sortBy(_.pos.modulus2).drop(index).headOption
    val shouldIMoveToBombPod    = maybeDestinationBombPod.fold(false)(!_.collides(me, startingTime))

    val actions: List[GameAction] = gameState.bosses.values.headOption match {
      case Some(theBoss) if !shouldIMoveToBombPod =>
        val distanceBetweenBossAndShadow =
          gameState.allTEntities[CreepingShadow].values.headOption.fold(Double.MaxValue) { creepingShadow =>
            creepingShadow.pos.distanceTo(theBoss.pos) - creepingShadow.shape.radius
          }

        val shouldIDamageZone = (distanceBetweenBossAndShadow < 200.0) && !isMyZoneThere(gameState, me)

        val rotationTowardsBoss = (theBoss.pos - me.pos).arg
        val maybePentagonBullet = Option
          .unless(shouldIDamageZone || theBoss.moving)(
            maybePentagonBulletUsage(gameState, startingTime, me, rotationTowardsBoss)
          )
          .flatten

        val maybeDamageZone =
          Option
            .when(shouldIDamageZone)(
              maybePentagonZoneUsage(
                gameState,
                startingTime,
                me,
                gameState.allTEntities[SmallGuy].values.map(_.pos).avg
              )
            )
            .flatten

        maybeDamageZone.orElse(maybePentagonBullet).toList
      case Some(_) if shouldIMoveToBombPod =>
        /*
          I need to move to one of the bomb pod.
          I will go to the one corresponding to my index. To that end, I sort
          all bomb pods by distance to the center, and I go to the one for which its
          position is my index.
          For example, if my index is 0, I move to the one closest to the origin.

          The probability that two of them are at the same distance is 0, so we ignore that case. But we could
          do some lexigographical order.
         */

        (for {
          destinationBombPod <- maybeDestinationBombPod
          targetPosition = destinationBombPod.pos
        } yield preGameMovement(startingTime, me, currentPosition, targetPosition, travelledDistance))
          .getOrElse(List.empty[GameAction])

      case None =>
        val targetPosition = -Boss110.halfWidth / 2 + Complex.rotation(index * math.Pi / 2 + math.Pi / 4) * 60
        preGameMovement(startingTime, me, currentPosition, targetPosition, travelledDistance)
    }

    sendActions(actions)

  }

}
