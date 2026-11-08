package application.ai.goodais.bosses.spiritofnara

import application.ai.goodais.classes.TriangleAIController
import application.ai.utils.maybeAbilityUsage
import gamelogic.abilities.Ability
import gamelogic.abilities.boss.spiritofnara.NaraNuke
import gamelogic.abilities.triangle.Cut
import gamelogic.entities.Entity
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.boss.firstraid.SpiritOfNara
import gamelogic.entities.classes.Triangle
import gamelogic.gamestate.gameactions.EntityStartsCasting
import gamelogic.gamestate.{GameAction, GameState}

/** The melee damage dealer stands behind the boss, and cuts its spells (locking it out of their
  * school) whenever it can.
  */
final case class TriangleForSpiritOfNara(index: Int, entityId: Entity.Id)
    extends TriangleAIController(index)
    with SpiritOfNaraBot[Triangle] {

  def homeAngle: Angle = Angle.zero

  def homeDistance: Double = SpiritOfNaraBot.meleeDistance

  def actionsInPlace(
      gameState: GameState,
      me: Triangle,
      boss: SpiritOfNara,
      time: Long
  ): Vector[GameAction] =
    maybeCutUsage(gameState, me, boss, time)
      .orElse(defaultAggressiveAbility(gameState, me, time, boss))
      .orElse(maybeEnergyKickUsage(gameState, time, me, boss))
      .toVector

  /** Cuts the spell of the boss, after a human-like reaction time. */
  private def maybeCutUsage(
      gameState: GameState,
      me: Triangle,
      boss: SpiritOfNara,
      time: Long
  ): Option[EntityStartsCasting] =
    gameState.castingEntityInfo
      .get(boss.id)
      .filter(info => info.ability.isInstanceOf[NaraNuke] && info.isCasting(time, 0))
      .filter(info => time - info.startedTime > TriangleForSpiritOfNara.reactionTime)
      .flatMap(_ =>
        maybeAbilityUsage(
          me,
          Cut(Ability.UseId.dummy, time, me.id, boss.id),
          gameState
        ).startCasting
      )

}

object TriangleForSpiritOfNara {
  inline def reactionTime: Long = 600L
}
