package application.ai.goodais.classes

import application.ai.goodais.GoodAIController
import gamelogic.entities.classes.Pentagon
import scala.reflect.ClassTag
import gamelogic.gamestate.GameState
import application.ai.utils.maybeAbilityUsage
import gamelogic.abilities.pentagon.CreatePentagonBullet
import gamelogic.physics.Complex
import gamelogic.gamestate.gameactions.boss102.PutDamageZone
import gamelogic.abilities.pentagon.CreatePentagonZone
import gamelogic.entities.classes.pentagon.PentagonZone
import utils.misc.RGBColour
import gamelogic.abilities.Ability
import models.bff.outofgame.gameconfig.PlayerName
import models.bff.outofgame.PlayerClasses

trait PentagonAIController(index: Int) extends GoodAIController[Pentagon] {
  final val classTag: ClassTag[Pentagon] = implicitly[ClassTag[Pentagon]]

  val name = PlayerName.AIPlayerName(PlayerClasses.Pentagon, index)

  final def maybePentagonBulletUsage(
      gameState: GameState,
      time: Long,
      me: Pentagon,
      direction: Double
  ) =
    maybeAbilityUsage(
      me,
      CreatePentagonBullet(
        Ability.UseId.dummy,
        time,
        me.id,
        me.pos,
        CreatePentagonBullet.damage,
        direction,
        me.colour
      ),
      gameState
    ).startCasting

  final def maybePentagonZoneUsage(
      gameState: GameState,
      time: Long,
      me: Pentagon,
      position: Complex
  ) =
    maybeAbilityUsage(
      me,
      CreatePentagonZone(
        Ability.UseId.dummy,
        time,
        me.id,
        position,
        PentagonZone.damageOnTick,
        (position - me.pos).arg,
        RGBColour.fromIntColour(me.colour).withAlpha(0.5)
      ),
      gameState
    ).startCasting

  def isMyZoneThere(gameState: GameState, me: Pentagon): Boolean =
    gameState.allTEntities[PentagonZone].values.exists(_.sourceId == me.id)

}
