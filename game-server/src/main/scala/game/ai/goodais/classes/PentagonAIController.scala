package game.ai.goodais.classes

import game.ai.goodais.GoodAIController
import gamelogic.entities.classes.Pentagon
import scala.reflect.ClassTag
import gamelogic.gamestate.GameState
import game.ai.utils.maybeAbilityUsage
import gamelogic.abilities.pentagon.CreatePentagonBullet

trait PentagonAIController extends GoodAIController[Pentagon] {
  final val classTag: ClassTag[Pentagon] = implicitly[ClassTag[Pentagon]]

  final def maybePentagonBulletUsage(gameState: GameState, time: Long, me: Pentagon, direction: Double) =
    maybeAbilityUsage(
      me,
      CreatePentagonBullet(
        0L,
        time,
        me.id,
        me.pos,
        CreatePentagonBullet.damage,
        direction,
        me.colour
      ),
      gameState
    ).startCasting
}
