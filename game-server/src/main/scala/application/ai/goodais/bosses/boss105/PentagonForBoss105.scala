package application.ai.goodais.bosses.boss105

import application.ai.goodais.classes.PentagonAIController
import gamelogic.buffs.boss.boss105.VolatileCharge
import gamelogic.entities.Entity
import gamelogic.entities.boss.boss105.Shade
import gamelogic.entities.classes.{Pentagon, Square}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex

/** The Pentagon dispels the [[VolatileCharge]]s (the ones of the tanks first), after a reaction
  * time similar to what a human would need, and shoots the Shade, or the boss when the Shade is
  * dead.
  */
final case class PentagonForBoss105(index: Int, entityId: Entity.Id)
    extends PentagonAIController(index)
    with Boss105Bot[Pentagon] {

  def restingPosition(gameState: GameState, me: Pentagon, time: Long): (Complex, Double) =
    Boss105Bot.rangedPosition(index) -> 10.0

  override def instantActions(
      gameState: GameState,
      me: Pentagon,
      time: Long
  ): Vector[GameAction] =
    gameState
      .allTBuffs[VolatileCharge]
      .filter(charge => time - charge.appearanceTime > reactionTime(charge))
      .toVector
      .sortBy(charge =>
        (
          !gameState.players.get(charge.bearerId).exists(_.isInstanceOf[Square]),
          charge.appearanceTime
        )
      )
      .iterator
      .flatMap(charge => maybeDispelUsage(gameState, time, charge.bearerId, me))
      .nextOption()
      .toVector

  def actionsInPlace(gameState: GameState, me: Pentagon, time: Long): Vector[GameAction] =
    Boss105Bot
      .focus(gameState)
      .filterNot(foe => foe.isInstanceOf[Shade] && Boss105Bot.holdFireOnShade(gameState, time))
      .flatMap { foe =>
        gameState.areTheyInSight(me.id, foe.id, time).filter(identity).flatMap { _ =>
          maybePentagonBulletUsage(
            gameState,
            time,
            me,
            (foe.currentPosition(time) - me.currentPosition(time)).arg
          )
        }
      }
      .toVector

  /** The charge has 8 seconds to live: a bit of time to notice it, and then the dispel. The time is
    * drawn once per charge (the buff id is the seed), otherwise re-drawing at each frame would
    * always end up with the shortest one.
    */
  private def reactionTime(charge: VolatileCharge): Long =
    PentagonForBoss105.minimumReactionTime +
      scala.util
        .Random(charge.buffId.hashCode.toLong)
        .nextLong(PentagonForBoss105.reactionTimeSpread) +
      index * PentagonForBoss105.secondPentagonExtraDelay

}

object PentagonForBoss105 {
  inline def minimumReactionTime: Long      = 900L
  inline def reactionTimeSpread: Long       = 800L
  inline def secondPentagonExtraDelay: Long = 600L
}
