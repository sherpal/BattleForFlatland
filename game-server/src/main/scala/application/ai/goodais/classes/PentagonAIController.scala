package application.ai.goodais.classes

import application.ai.goodais.GoodAIController
import application.ai.utils.maybeAbilityUsage
import gamelogic.abilities.Ability
import gamelogic.abilities.pentagon.{
  CreatePentagonBullet,
  CreatePentagonZone,
  Fracture,
  PentaDispel,
  Pentagram
}
import gamelogic.buffs.abilities.classes.Fractured
import gamelogic.entities.{Entity, MovingBody}
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.classes.Pentagon
import gamelogic.entities.classes.pentagon.PentagonZone
import gamelogic.gamestate.GameState
import gamelogic.gamestate.gameactions.EntityStartsCasting
import gamelogic.physics.Complex
import models.bff.outofgame.PlayerClasses
import models.bff.outofgame.gameconfig.PlayerName
import utils.misc.RGBColour

import scala.reflect.ClassTag

trait PentagonAIController(index: Int) extends GoodAIController[Pentagon] {
  final val classTag: ClassTag[Pentagon] = implicitly[ClassTag[Pentagon]]

  val name = PlayerName.AIPlayerName(PlayerClasses.Pentagon, index)

  final def maybePentagonBulletUsage(
      gameState: GameState,
      time: Long,
      me: Pentagon,
      direction: Angle
  ): Option[EntityStartsCasting] =
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

  /** Pentagram when our facets are all lit, Fracture when `foe` is not fractured by us anymore (or
    * soon not anymore), and Pentagon Bullet otherwise.
    */
  final def maybeDamageUsage(
      gameState: GameState,
      time: Long,
      me: Pentagon,
      foe: MovingBody
  ): Option[EntityStartsCasting] = {
    val direction = (foe.currentPosition(time) - me.currentPosition(time)).arg

    def maybePentagram =
      maybeAbilityUsage(me, Pentagram(Ability.UseId.dummy, time, me.id, foe.id), gameState)

    def maybeFracture = Option
      .unless(
        Fractured
          .by(gameState, foe.id, me.id)
          .exists(fractured =>
            fractured.appearanceTime + fractured.duration - time > Fractured.maxCarriedOver
          )
      )(
        maybeAbilityUsage(
          me,
          Fracture(
            Ability.UseId.dummy,
            time,
            me.id,
            me.pos,
            Fracture.damage,
            direction,
            me.colour
          ),
          gameState
        )
      )
      .flatten

    maybePentagram.startCasting
      .orElse(maybeFracture.startCasting)
      .orElse(maybePentagonBulletUsage(gameState, time, me, direction))
  }

  final def maybeDispelUsage(
      gameState: GameState,
      time: Long,
      targetId: Entity.Id,
      me: Pentagon
  ) = maybeAbilityUsage(
    me,
    PentaDispel(Ability.UseId.dummy, time, me.id, targetId),
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
