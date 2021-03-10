package game.ai.goodais.boss.boss101

import gamelogic.entities.Entity
import models.bff.outofgame.gameconfig.PlayerName
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorRef
import gamelogic.gamestate.GameState
import models.bff.outofgame.PlayerClasses
import game.ActionTranslator
import game.ai.goodais.boss.GoodAICreator.ActionTranslatorRef
import gamelogic.entities.classes.Square
import game.ai.goodais.GoodAIController
import zio.duration._
import gamelogic.entities.boss.Boss101
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.gameactions.MovingBodyMoves
import game.ai.utils.maybeAbilityUsage
import gamelogic.abilities.square.Taunt
import gamelogic.abilities.square.Enrage
import gamelogic.buffs.Buff
import scala.reflect.ClassTag
import gamelogic.abilities.square.HammerHit
import game.ai.goodais.classes.SquareAIController

object SquareForBoss101 extends SquareAIController {

  val name: PlayerName.AIPlayerName = PlayerName.AIPlayerName(PlayerClasses.Square, 0)

  def loop(
      startingTime: Long,
      gameState: GameState,
      me: Square,
      sendActions: List[GameAction] => Unit
  ): Unit = {
    import gamelogic.physics.Complex.DoubleWithI
    val previousPosition  = me.pos
    val currentPosition   = me.currentPosition(startingTime)
    val travelledDistance = previousPosition distanceTo currentPosition

    val actions: List[GameAction] = gameState.bosses.values.headOption match {
      case Some(theBoss) =>
        val shouldIHammer = shouldIHammerTheBoss(theBoss, me)

        val maybeTaunt = Option
          .unless(shouldIHammer)(maybeTauntUsage(gameState, startingTime, me, theBoss))
          .flatten
        val alreadyEnraged = gameState.allBuffsOfEntity(me.id).exists(_.resourceIdentifier == Buff.squareEnrage)
        val maybeEnrage = maybeEnrageUsage(
          gameState,
          startingTime,
          me,
          me.resourceAmount.amount < 5 && me.life > 180 && !alreadyEnraged
        )

        val maybeHammerHit = Option
          .when(shouldIHammer)(maybeHammerHitUsage(gameState, startingTime, me, theBoss))
          .flatten
        maybeHammerHit.orElse(maybeTaunt).orElse(maybeEnrage).toList
      case None =>
        val targetPosition = Boss101.bossStartingPosition - 50.i
        preGameMovement(startingTime, me, currentPosition, targetPosition, travelledDistance)
    }

    sendActions(actions)
  }

}
