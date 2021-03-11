package game.ai.goodais.boss.boss110

import game.ai.goodais.classes.TriangleAIController
import models.bff.outofgame.gameconfig.PlayerName
import gamelogic.entities.classes.Triangle
import gamelogic.gamestate.{GameAction, GameState}
import models.bff.outofgame.PlayerClasses
import gamelogic.abilities.WithTargetAbility
import gamelogic.gamestate.gameactions.MovingBodyMoves
import gamelogic.physics.Complex
import gamelogic.entities.boss.dawnoftime.Boss110
import gamelogic.entities.boss.boss110.BigGuy
import gamelogic.buffs.abilities.classes.TriangleStunDebuff
import gamelogic.abilities.triangle.Stun

final class TriangleForBoss110(index: Int) extends TriangleAIController {

  val name: PlayerName.AIPlayerName = PlayerName.AIPlayerName(PlayerClasses.Triangle, index)

  def loop(startingTime: Long, gameState: GameState, me: Triangle, sendActions: List[GameAction] => Unit): Unit = {
    import gamelogic.physics.Complex.DoubleWithI
    val (previousPosition, currentPosition) = someDistanceInfo(startingTime, me)
    val travelledDistance                   = previousPosition distanceTo currentPosition

    val bigGuies = gameState.allTEntities[BigGuy].values.toList.sortBy(_.pos.im)

    def doesBigGuyHaveMyCC(bigGuy: BigGuy): Boolean =
      gameState
        .allBuffsOfEntity(bigGuy.id)
        .collectFirst {
          case buff: TriangleStunDebuff if buff.sourceId == me.id => buff
        }
        .isDefined
    val (maybeBigGuyToCC, maybeBigGuyToAttack) = bigGuies match {
      case Nil           => (None, None)
      case bigGuy :: Nil => (None, Some(bigGuy))
      case toAttack :: toCC :: Nil =>
        (if (index == 1) Some(toCC).filterNot(doesBigGuyHaveMyCC) else None, Some(toAttack))
      case toCCFor0 :: toAttack :: toCCFor1 :: Nil =>
        ((if (index == 0) Some(toCCFor0) else Some(toCCFor1)).filterNot(doesBigGuyHaveMyCC), Some(toAttack))
      case toAttack :: _ =>
        (None, Some(toAttack)) // we are probably dead anyway
    }

    val actions: List[GameAction] = (gameState.bosses.values.headOption, maybeBigGuyToCC) match {
      case (_, Some(bigGuy)) =>
        maybeStunUsage(gameState, startingTime, me, bigGuy).toList
      case (Some(theBoss), _)
          if maybeBigGuyToAttack.getOrElse(theBoss).pos.distanceTo(currentPosition) < WithTargetAbility.meleeRange =>
        val target = maybeBigGuyToAttack.getOrElse(theBoss)

        val maybeAggressiveAttack = defaultAggressiveAbility(gameState, me, startingTime, target)
        val maybeFiller           = maybeEnergyKickUsage(gameState, startingTime, me, target)

        List(
          stopMoving(startingTime, me, currentPosition, (target.pos - currentPosition).arg),
          maybeFiller,
          maybeAggressiveAttack.filter(cast => me.resourceAmount >= Stun.cost + cast.ability.cost)
        ).flatten

      case (Some(theBoss), _) =>
        val target         = maybeBigGuyToAttack.getOrElse(theBoss)
        val targetPosition = target.pos + target.shape.radius + me.shape.radius
        preGameMovement(startingTime, me, currentPosition, targetPosition, travelledDistance)
      case (None, _) =>
        val targetPosition = Complex(Boss110.halfWidth / 2, (index - 0.5) * Boss110.halfHeight)
        preGameMovement(startingTime, me, currentPosition, targetPosition, travelledDistance)
    }

    sendActions(actions)

  }

}
