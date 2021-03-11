package game.ai.goodais.classes

import game.ai.goodais.GoodAIController
import gamelogic.entities.classes.Square
import scala.reflect.ClassTag
import gamelogic.entities.boss.BossEntity
import game.ai.utils.maybeAbilityUsage
import gamelogic.gamestate.GameState
import gamelogic.entities.Entity
import gamelogic.abilities.square.Taunt
import gamelogic.abilities.square.Enrage
import gamelogic.abilities.square.HammerHit
import gamelogic.abilities.square.Cleave
import gamelogic.buffs.Buff
import gamelogic.entities.WithThreat

trait SquareAIController extends GoodAIController[Square] {
  final val classTag: ClassTag[Square] = implicitly[ClassTag[Square]]

  def shouldIHammerThisTarget(target: WithThreat, me: Square, threshold: Double = 2000): Boolean =
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
    maybeAbilityUsage(me, Taunt(0L, time, me.id, target.id), gameState).startCasting

  final def maybeEnrageUsage(gameState: GameState, time: Long, me: Square, condition: Boolean) =
    Option
      .when(condition)(
        maybeAbilityUsage(me, Enrage(0L, time, me.id), gameState).startCasting
      )
      .flatten

  final def maybeHammerHitUsage(gameState: GameState, time: Long, me: Square, target: Entity) =
    maybeAbilityUsage(me, HammerHit(0L, time, me.id, target.id), gameState).startCasting

  final def maybeCleaveUsage(gameState: GameState, time: Long, me: Square, direction: Double) =
    maybeAbilityUsage(me, Cleave(0L, time, me.id, me.pos, direction), gameState).startCasting

}
