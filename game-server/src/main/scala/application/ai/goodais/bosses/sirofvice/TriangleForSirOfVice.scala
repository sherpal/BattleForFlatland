package application.ai.goodais.bosses.sirofvice

import application.ai.goodais.classes.TriangleAIController
import gamelogic.entities.Entity
import gamelogic.entities.boss.firstraid.SirOfVice
import gamelogic.entities.classes.{Constants, Triangle}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex

/** The melee damage dealer stands behind the boss (with respect to the tank) and hits it. */
final case class TriangleForSirOfVice(index: Int, entityId: Entity.Id)
    extends TriangleAIController(index)
    with SirOfViceBot[Triangle] {

  def isMelee: Boolean = true

  def restingPosition(
      gameState: GameState,
      me: Triangle,
      maybeBoss: Option[SirOfVice],
      time: Long
  ): (Complex, Double) = maybeBoss match {
    case None => SirOfViceBot.tankPosition + Complex(0, -60) -> 10.0
    case Some(boss) =>
      val bossPosition = boss.currentPosition(time)
      val tankPosition = gameState.players
        .get(boss.targetId)
        .filterNot(_.id == me.id)
        .fold(SirOfViceBot.tankPosition)(_.currentPosition(time))
      val behind = bossPosition +
        (bossPosition - tankPosition).safeNormalized * (Constants.bossRadius + Constants.playerRadius + 5)
      behind -> 8.0
  }

  def actionsInPlace(
      gameState: GameState,
      me: Triangle,
      boss: SirOfVice,
      time: Long
  ): Vector[GameAction] =
    defaultAggressiveAbility(gameState, me, time, boss)
      .orElse(maybeEnergyKickUsage(gameState, time, me, boss))
      .toVector

}
