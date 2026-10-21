package application.ai.goodais.bosses.boss105

import application.ai.goodais.classes.TriangleAIController
import gamelogic.abilities.Ability
import gamelogic.abilities.boss.boss105.Resonance
import gamelogic.abilities.triangle.{Cut, DirectHit, Stun}
import gamelogic.entities.Entity
import gamelogic.entities.boss.boss105.Shade
import gamelogic.entities.boss.dawnoftime.Boss105
import gamelogic.entities.classes.Triangle
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex

/** The Triangle stands behind the Shade (or the boss, when the Shade is dead) with respect to the
  * tank holding it, hits it, and interrupts its [[gamelogic.abilities.boss.boss105.Siphon]].
  */
final case class TriangleForBoss105(index: Int, entityId: Entity.Id)
    extends TriangleAIController(index)
    with Boss105Bot[Triangle] {

  def restingPosition(gameState: GameState, me: Triangle, time: Long): (Complex, Double) =
    Boss105Bot.focus(gameState) match {
      case None =>
        Boss105Bot.rangedPosition(index) -> 10.0
      case Some(foe) =>
        val foePosition = foe.currentPosition(time)
        val tankPosition = gameState.players
          .get(foe.targetId)
          .filterNot(_.id == me.id)
          .map(_.currentPosition(time))
          .getOrElse(
            if foe.isInstanceOf[Shade] then
              Boss105Bot.shadeTankPosition(
                Boss105Bot
                  .maybeShadeCorner(gameState, time)
                  .getOrElse(Boss105.shadeStartingPosition)
              )
            else Boss105Bot.bossTankPosition
          )
        val distance = foe match {
          case shade: Shade => shade.shape.radius + 10
          case _            => gamelogic.entities.classes.Constants.bossRadius + 15
        }
        (foePosition + (foePosition - tankPosition).safeNormalized * distance) -> 8.0
    }

  override def instantActions(gameState: GameState, me: Triangle, time: Long): Vector[GameAction] =
    Boss105Bot.focus(gameState).fold(Vector.empty[GameAction]) { foe =>
      def maybeCutSiphon = for {
        shade <- Boss105Bot.theShade(gameState)
        info  <- gameState.castingEntityInfo.get(shade.id)
        if info.isCasting(time, 0L)
        if info.remainingCastingTime(time, 0L) < info.castingTime / 2
        ability <- application.ai.utils.maybeAbilityUsage(
          me,
          Cut(Ability.UseId.dummy, time, me.id, shade.id),
          gameState
        )
      } yield ability.toStartCasting(time)

      // the stun that holds the Shade when the boss calls it, which nobody should break
      def maybeStunShade = for {
        shade <- Boss105Bot.theShade(gameState)
        if Boss105Bot.shadeIsYearning(gameState) && !Boss105Bot.shadeIsHeldByStun(gameState)
        if Boss105Bot
          .theBoss(gameState)
          .forall(_.currentPosition(time).distanceTo(shade.currentPosition(time)) > Resonance.range)
        stun <- maybeStunUsage(gameState, time, me, shade)
      } yield stun

      // the stun costs energy: some must be left when the boss calls the Shade
      val energyToSave = if Boss105Bot.reunionIsNear(gameState, time) then Stun.cost.amount else 0.0

      def maybeAttack =
        Option
          .unless(foe.isInstanceOf[Shade] && Boss105Bot.holdFireOnShade(gameState, time))(
            Option
              .when(me.resourceAmount.amount >= DirectHit.cost.amount + energyToSave)(
                defaultAggressiveAbility(gameState, me, time, foe)
              )
              .flatten
              .orElse(maybeEnergyKickUsage(gameState, time, me, foe))
          )
          .flatten

      maybeCutSiphon.orElse(maybeStunShade).orElse(maybeAttack).toVector
    }

  def actionsInPlace(gameState: GameState, me: Triangle, time: Long): Vector[GameAction] =
    Vector.empty

}
