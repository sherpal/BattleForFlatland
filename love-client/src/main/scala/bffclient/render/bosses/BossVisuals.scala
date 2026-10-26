package bffclient.render.bosses

import bffclient.render.Painter
import bffclient.ui.Colour as Paint
import gamelogic.entities.{Body, Entity, EntityCastingInfo, LivingEntity}
import gamelogic.gamestate.GameState
import utils.misc.RGBColour

/** What the frontend draws specifically for a boss (its `bossspecificdrawers`): adds, zones, auras,
  * beams, ...
  */
trait BossVisuals {

  /** The name of the boss (`BossEntity.name`). */
  def bossName: String

  /** Zones, auras and the like, under the bodies. */
  def ground(p: Painter): Unit

  /** The adds of the boss. */
  def adds(p: Painter): Unit

  /** Drawn by `ground` or `adds` (so the renderer does not draw them as other bodies). */
  def draws(entity: Entity): Boolean

  /** How to tint the frame of the player in the HUD, like the frontend's `BossGUIPlugin`s: the
    * colour of the contour, and of the background with its opacity.
    */
  def playerFrameTint(gameState: GameState, playerId: Entity.Id): Option[(Paint, Double)] = None

  /** A boss-specific line of the HUD, like the frontend's `bossspecificcomponents`. */
  def hudText(gameState: GameState): Option[String] = None

  protected def paint(colour: RGBColour, alpha: Double = 1.0): Paint =
    Paint.rgb(colour.red, colour.green, colour.blue).withAlpha(alpha)

  protected val addColour: Paint = Paint.rgb(50, 50, 50)

  /** The bars and buff icons of an add, like those of the boss. */
  protected def withBars(p: Painter, add: LivingEntity & Body): Unit = {
    p.lifeBar(add)
    p.castingBar(add)
    p.buffIcons(add)
  }

  protected def casting(p: Painter, casterId: Entity.Id): Option[EntityCastingInfo] =
    p.gameState.castingEntityInfo.get(casterId).filter(_.isCasting(p.now, 0L))

  protected def progress(info: EntityCastingInfo, now: Long): Double =
    math.min(
      1.0,
      math.max(0.0, 1.0 - info.remainingCastingTime(now, 0L).toDouble / info.castingTime)
    )
}

object BossVisuals {

  /** By boss name, like the frontend's `drawerMapping`. Boss 101 has no visuals, and Boss 110's
    * are not done (it is broken).
    */
  private val byBossName: Map[String, BossVisuals] =
    Seq(Boss102Visuals, Boss104Visuals, Boss105Visuals, SirOfViceVisuals).map(v => v.bossName -> v).toMap

  /** The visuals of the boss being fought, if it has any. */
  def of(gameState: GameState): Option[BossVisuals] =
    gameState.bosses.values.iterator.flatMap(boss => byBossName.get(boss.name)).nextOption()
}
