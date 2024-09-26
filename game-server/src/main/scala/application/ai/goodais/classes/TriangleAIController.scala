package application.ai.goodais.classes

import application.ai.goodais.GoodAIController
import gamelogic.entities.classes.Triangle
import gamelogic.gamestate.gameactions.EntityStartsCasting
import gamelogic.gamestate.GameState
import gamelogic.buffs.Buff
import gamelogic.abilities.triangle.UpgradeDirectHit
import application.ai.utils.maybeAbilityUsage
import gamelogic.abilities.triangle.DirectHit
import gamelogic.entities.boss.BossEntity
import gamelogic.entities.Entity
import scala.reflect.ClassTag
import gamelogic.abilities.triangle.EnergyKick
import gamelogic.abilities.triangle.Stun
import gamelogic.abilities.Ability
import models.bff.outofgame.gameconfig.PlayerName
import models.bff.outofgame.PlayerClasses

trait TriangleAIController(index: Int) extends GoodAIController[Triangle] {

  final val classTag: ClassTag[Triangle] = implicitly[ClassTag[Triangle]]

  val name = PlayerName.AIPlayerName(PlayerClasses.Triangle, index)

  def defaultAggressiveAbility(
      gameState: GameState,
      me: Triangle,
      time: Long,
      target: Entity
  ): Option[EntityStartsCasting] = {
    val shouldIBuffMyself =
      !gameState
        .allBuffsOfEntity(me.id)
        .exists(_.resourceIdentifier == Buff.triangleUpgradeDirectHit)

    val maybeBuffMyself =
      Option
        .when(shouldIBuffMyself)(
          maybeAbilityUsage(
            me,
            UpgradeDirectHit(Ability.UseId.dummy, time, me.id),
            gameState
          ).startCasting
        )
        .flatten

    val maybeDirectHit =
      Option
        .unless(shouldIBuffMyself)(
          maybeAbilityUsage(
            me,
            DirectHit(Ability.UseId.dummy, time, me.id, target.id, DirectHit.directHitDamage),
            gameState
          ).startCasting
        )
        .flatten

    maybeBuffMyself.orElse(maybeDirectHit)
  }

  def maybeEnergyKickUsage(gameState: GameState, time: Long, me: Triangle, target: Entity) =
    maybeAbilityUsage(
      me,
      EnergyKick(Ability.UseId.dummy, time, me.id, target.id),
      gameState
    ).startCasting

  def maybeStunUsage(gameState: GameState, time: Long, me: Triangle, target: Entity) =
    maybeAbilityUsage(me, Stun(Ability.UseId.dummy, time, me.id, target.id), gameState).startCasting

}
