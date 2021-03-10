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

object SquareForBoss101 extends GoodAIController[Square] {

  val name: PlayerName.AIPlayerName = PlayerName.AIPlayerName(PlayerClasses.Square, 0)

  val classTag: ClassTag[Square] = implicitly[ClassTag[Square]]

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
        val shouldIHammer = (for {
          myThreatToBoss <- theBoss.damageThreats.get(me.id)
          threatsFromOthers = theBoss.damageThreats.filterNot(_._1 == me.id).values.toList
          if threatsFromOthers.nonEmpty
          secondThreat <- threatsFromOthers.sorted.reverse.headOption
          if secondThreat < myThreatToBoss - 2000
        } yield ()).isDefined

        val maybeTaunt = Option
          .unless(shouldIHammer)(
            maybeAbilityUsage(me, Taunt(0L, startingTime, me.id, theBoss.id), gameState).startCasting
          )
          .flatten
        val alreadyEnraged = gameState.allBuffsOfEntity(me.id).exists(_.resourceIdentifier == Buff.squareEnrage)
        val maybeEnrage =
          Option
            .when(me.resourceAmount.amount < 5 && me.life > 180 && !alreadyEnraged)(
              maybeAbilityUsage(me, Enrage(0L, startingTime, me.id), gameState).startCasting
            )
            .flatten

        val maybeHammerHit = Option
          .when(shouldIHammer)(
            maybeAbilityUsage(me, HammerHit(0L, startingTime, me.id, theBoss.id), gameState).startCasting
          )
          .flatten
        maybeHammerHit.orElse(maybeTaunt).orElse(maybeEnrage).toList
      case None =>
        val targetPosition = Boss101.bossStartingPosition - 50.i
        preGameMovement(startingTime, me, currentPosition, targetPosition, travelledDistance)
    }

    sendActions(actions)
  }

}
