package bffclient.render.bosses

import bffclient.love.Graphics
import bffclient.render.{Painter, Sprites}
import bffclient.ui.Colour as Paint
import gamelogic.buffs.boss.boss102.LivingDamageZone
import gamelogic.entities.Entity
import gamelogic.entities.boss.boss102.{BossHound, DamageZone}
import gamelogic.entities.boss.dawnoftime.Boss102
import gamelogic.gamestate.GameState

/** Boss 102: its hounds, its damage zones, and the zone around the players who carry one. */
object Boss102Visuals extends BossVisuals {
  val bossName: String = Boss102.name

  def draws(entity: Entity): Boolean = entity match {
    case _: BossHound | _: DamageZone => true
    case _                            => false
  }

  def ground(p: Painter): Unit = {
    import p.*
    for zone <- gameState.allTEntities[DamageZone].values do {
      circle(zone.pos, DamageZone.radius, Some(Paint(0, 1, 0, 0.5)))
      for sprite <- Sprites.damageZone do {
        val (x, y) = toScreen(zone.pos)
        Graphics.setColor(0.5, 0.5, 0, 0.3) // olive
        sprite.draw(x, y, 0, DamageZone.radius * 4, frame = ((now / 100) % sprite.frameCount).toInt)
      }
    }
    for
      (id, player) <- gameState.players
      if gameState.allBuffsOfEntity(id).exists(_.isInstanceOf[LivingDamageZone])
    do Sprites.livingDamageZone match {
      case Some(sprite) =>
        val (x, y) = toScreen(position(player))
        Graphics.setColor(1, 1, 1, 0.3)
        sprite.draw(
          x,
          y,
          0,
          2 * LivingDamageZone.range * 192 / 120,
          frame = ((now / 50) % sprite.frameCount).toInt
        )
      case None =>
        circle(
          position(player),
          LivingDamageZone.range,
          Some(Paint(0.8, 0, 0, 0.2)),
          Some((2, Paint(0.8, 0, 0, 0.6)))
        )
    }
  }

  def adds(p: Painter): Unit =
    for hound <- p.gameState.allTEntities[BossHound].values do {
      p.shape(hound, Some(addColour.withAlpha(0.3)), Some((2, addColour)))
      withBars(p, hound)
    }

  override def hudText(gameState: GameState): Option[String] =
    Some(s"Hounds: ${gameState.allTEntities[BossHound].size}")
}
