package game.ai.boss.boss110units

import game.ai.boss.SimpleAIController
import gamelogic.entities.boss.boss110.SmallGuy
import gamelogic.gamestate.gameactions.boss110.AddSmallGuy
import scala.reflect.ClassTag
import gamelogic.gamestate.gameactions.EntityStartsCasting
import gamelogic.gamestate.GameState

object SmallGuyController extends SimpleAIController[SmallGuy, AddSmallGuy] {

  def meleeRange: Double = SmallGuy.range
  def fullSpeed: Double  = SmallGuy.fullSpeed

  def actions(gameState: GameState, me: SmallGuy, time: Long): List[Option[EntityStartsCasting]] =
    List(me.maybeAutoAttack(time).startCasting.filter(_.isLegalBoolean(gameState)))

  val classTag: ClassTag[SmallGuy] = implicitly[ClassTag[SmallGuy]]

}
