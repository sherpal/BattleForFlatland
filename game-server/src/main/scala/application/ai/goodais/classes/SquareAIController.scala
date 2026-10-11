package application.ai.goodais.classes

import application.ai.goodais.GoodAIController
import application.ai.utils.maybeAbilityUsage
import gamelogic.abilities.Ability
import gamelogic.abilities.square.{Cleave, Enrage, HammerHit, Taunt}
import gamelogic.buffs.Buff
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.WithThreat.ThreatAmount
import gamelogic.entities.classes.Square
import gamelogic.entities.{Entity, WithThreat}
import gamelogic.gamestate.GameState
import gamelogic.gamestate.gameactions.EntityStartsCasting
import models.bff.outofgame.PlayerClasses
import models.bff.outofgame.gameconfig.PlayerName

import scala.math.Ordered.orderingToOrdered
import scala.reflect.ClassTag

trait SquareAIController(index: Int) extends GoodAIController[Square] {
  final val classTag: ClassTag[Square] = implicitly[ClassTag[Square]]

  val name = PlayerName.AIPlayerName(PlayerClasses.Square, index)

  def isMyThreatTowardsTargetEnough(
      target: WithThreat,
      me: Square,
      threshold: ThreatAmount = ThreatAmount.fromValue(2000)
  ): Boolean =
    (for {
      myThreatToBoss <- target.damageThreats.get(me.id)
      threatsFromOthers = target.damageThreats.filterNot(_._1 == me.id).values.toList
      if threatsFromOthers.nonEmpty
      secondThreat <- threatsFromOthers.sorted.reverse.headOption
      if secondThreat < myThreatToBoss - threshold
    } yield ()).isDefined

  def alreadyEnraged(gameState: GameState, me: Square): Boolean =
    buffsOnMe(gameState, me).exists(_.resourceIdentifier == Buff.squareEnrage)

  final def maybeTauntUsage(gameState: GameState, time: Long, me: Square, target: Entity) =
    maybeAbilityUsage(
      me,
      Taunt(Ability.UseId.dummy, time, me.id, target.id),
      gameState
    ).startCasting

  final def maybeEnrageUsage(gameState: GameState, time: Long, me: Square, condition: Boolean) =
    Option
      .when(condition)(
        maybeAbilityUsage(me, Enrage(Ability.UseId.dummy, time, me.id), gameState).startCasting
      )
      .flatten

  final def maybeHammerHitUsage(gameState: GameState, time: Long, me: Square, target: Entity) =
    maybeAbilityUsage(
      me,
      HammerHit(Ability.UseId.dummy, time, me.id, target.id),
      gameState
    ).startCasting

  final def maybeCleaveUsage(
      gameState: GameState,
      time: Long,
      me: Square,
      direction: Angle
  ): Option[EntityStartsCasting] =
    maybeAbilityUsage(
      me,
      Cleave(Ability.UseId.dummy, time, me.id, me.pos, direction),
      gameState
    ).startCasting

}
