package bffclient.render.bosses

import bffclient.render.Painter
import bffclient.ui.Colour as Paint
import gamelogic.buffs.boss.boss104.TwinDebuff
import gamelogic.entities.Entity
import gamelogic.entities.boss.boss104.{BigGuy, BigGuyDeathMark, DeathMarkTriangle, DebuffCircle}
import gamelogic.entities.boss.dawnoftime.Boss104
import gamelogic.gamestate.GameState
import utils.misc.RGBColour

/** Boss 104: its big guys, the circles of the twin debuffs, the debuffs on the players, the marks
  * left by the big guys and the triangle of the finisher (red while inside is deadly, green once it
  * is the only safe place).
  */
object Boss104Visuals extends BossVisuals {
  val bossName: String = Boss104.name

  def draws(entity: Entity): Boolean = entity match {
    case _: BigGuy | _: DebuffCircle | _: BigGuyDeathMark | _: DeathMarkTriangle => true
    case _                                                                       => false
  }

  def ground(p: Painter): Unit = {
    import p.*
    for debuffCircle <- gameState.allTEntities[DebuffCircle].values do
      circle(debuffCircle.pos, debuffCircle.shape.radius, Some(paint(debuffCircle.colour, 0.5)))
    for
      buff   <- gameState.allTBuffs[TwinDebuff]
      player <- gameState.players.get(buff.bearerId)
    do
      circle(position(player), player.shape.radius * 1.5, Some(paint(buff.colour, 0.5)))
    for mark <- gameState.allTEntities[BigGuyDeathMark].values do
      circle(mark.pos, mark.shape.radius, Some(Paint(0, 0, 0, 0.5)))
    for triangle <- gameState.allTEntities[DeathMarkTriangle].values do {
      val colour = if triangle.insideIsDeadly then RGBColour.red else RGBColour.green
      polygon(triangle.vertices, Some(paint(colour, 0.15)), Some((3, paint(colour))))
    }
  }

  def adds(p: Painter): Unit =
    for bigGuy <- p.gameState.allTEntities[BigGuy].values do {
      p.shape(bigGuy, Some(addColour.withAlpha(0.3)), Some((2, addColour)))
      withBars(p, bigGuy)
    }

  /** The twin debuffs must be dispelled from inside the circle of their colour. */
  override def playerFrameTint(gameState: GameState, playerId: Entity.Id): Option[(Paint, Double)] =
    gameState.allBuffsOfEntity(playerId).collectFirst { case debuff: TwinDebuff =>
      (paint(debuff.colour), 0.45)
    }
}
