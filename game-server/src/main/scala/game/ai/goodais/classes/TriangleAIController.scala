package game.ai.goodais.classes

import game.ai.goodais.GoodAIController
import gamelogic.entities.classes.Triangle
import gamelogic.gamestate.gameactions.EntityStartsCasting
import gamelogic.gamestate.GameState
import gamelogic.buffs.Buff
import gamelogic.abilities.triangle.UpgradeDirectHit
import game.ai.utils.maybeAbilityUsage
import gamelogic.abilities.triangle.DirectHit
import gamelogic.entities.boss.BossEntity
import gamelogic.entities.Entity
import scala.reflect.ClassTag

trait TriangleAIController extends GoodAIController[Triangle] {

  final val classTag: ClassTag[Triangle] = implicitly[ClassTag[Triangle]]

  def defaultAggressiveAbility(
      gameState: GameState,
      me: Triangle,
      time: Long,
      target: Entity
  ): Option[EntityStartsCasting] = {
    val shouldIBuffMyself =
      !gameState.allBuffsOfEntity(me.id).exists(_.resourceIdentifier == Buff.triangleUpgradeDirectHit)

    val maybeBuffMyself =
      Option
        .when(shouldIBuffMyself)(
          maybeAbilityUsage(me, UpgradeDirectHit(0L, time, me.id), gameState).startCasting
        )
        .flatten

    val maybeDirectHit =
      Option
        .unless(shouldIBuffMyself)(
          maybeAbilityUsage(
            me,
            DirectHit(0L, time, me.id, target.id, DirectHit.directHitDamage),
            gameState
          ).startCasting
        )
        .flatten

    maybeBuffMyself.orElse(maybeDirectHit)
  }

}
