package game.ai.goodais.boss.boss110

import game.ai.goodais.classes.SquareAIController
import models.bff.outofgame.gameconfig.PlayerName
import gamelogic.entities.classes.Square
import gamelogic.gamestate.{GameAction, GameState}
import models.bff.outofgame.PlayerClasses
import gamelogic.abilities.boss.boss110.SpawnBigGuies
import gamelogic.entities.boss.boss110.BigGuy
import gamelogic.buffs.Buff

/**
  * This tank will be responsible for tanking the boss, and the three [[BigGuy]] that spawn
  * from time to time.
  * This tank will move to the right of the game.
  */
object SquareToTheRightBoss110 extends SquareAIController {

  val name: PlayerName.AIPlayerName = PlayerName.AIPlayerName(PlayerClasses.Square, 1)

  def loop(startingTime: Long, gameState: GameState, me: Square, sendActions: List[GameAction] => Unit): Unit = {

    val actions: List[GameAction] = gameState.bosses.values.headOption match {
      case Some(theBoss) =>
        val bigGuies = gameState.allTEntities[BigGuy].values.toList.sortBy(_.pos.im)

        val maybeEnrage = maybeEnrageUsage(
          gameState,
          startingTime,
          me,
          me.resourceAmount.amount < 5 && me.life > 180 && !alreadyEnraged(gameState, me) && !buffsOnMe(gameState, me)
            .exists(
              _.resourceIdentifier == Buff.boss110BrokenArmor
            )
        )

        val target = bigGuies match {
          case Nil                     => theBoss // no big guies, target the boss
          case bigGuy :: Nil           => bigGuy // there is a big guy, we go for him
          case bigGuy :: _ :: Nil      => bigGuy // two big guies, we attack the one above
          case _ :: bigGuy :: _ :: Nil => bigGuy // three big guies, we attack the one in the middle
          case bigGuy :: _             => bigGuy // we are probably going to die soon anyway
        }

        val shouldIHammer = shouldIHammerThisTarget(target, me, threshold = 10000)
        val maybeTaunt = Option
          .unless(shouldIHammer)(maybeTauntUsage(gameState, startingTime, me, target))
          .flatten
        val maybeHammerHit = Option
          .when(shouldIHammer)(maybeHammerHitUsage(gameState, startingTime, me, target))
          .flatten
        maybeHammerHit.orElse(maybeTaunt).orElse(maybeEnrage).toList

      case None =>
        val (previousPosition, currentPosition) = someDistanceInfo(startingTime, me)
        val travelledDistance                   = previousPosition distanceTo currentPosition
        val targetPosition                      = SpawnBigGuies.bigGuiesPositions.avg - BigGuy.shape.radius
        preGameMovement(startingTime, me, currentPosition, targetPosition, travelledDistance)
    }

    sendActions(actions)
  }

}
