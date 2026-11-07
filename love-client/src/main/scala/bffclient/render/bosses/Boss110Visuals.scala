package bffclient.render.bosses

import bffclient.render.Painter
import bffclient.ui.Colour as Paint
import gamelogic.abilities.boss.boss110.ExplodeBombs
import gamelogic.buffs.boss.boss110.BrokenArmor
import gamelogic.entities.Entity
import gamelogic.entities.boss.boss110.{BigGuy, BombPod, CreepingShadow, SmallGuy}
import gamelogic.entities.boss.dawnoftime.Boss110
import gamelogic.gamestate.GameState

/** Boss 110: its creeping shadow (with a red edge while it touches the boss), the bombs (green when
  * covered by exactly one player, red otherwise, with a ring closing in until they explode), its
  * big and small guys, and the broken armours on the player frames.
  */
object Boss110Visuals extends BossVisuals {
  val bossName: String = Boss110.name

  private val shadowColour      = Paint(0.05, 0.05, 0.1)
  private val touchingColour    = Paint.rgb(230, 30, 60)
  private val coveredColour     = Paint.rgb(80, 200, 80)
  private val uncoveredColour   = Paint.rgb(230, 50, 30)
  private val smallGuyColour    = Paint.rgb(110, 110, 110)
  private val brokenArmorColour = Paint.rgb(255, 140, 0)

  def draws(entity: Entity): Boolean = entity match {
    case _: BigGuy | _: SmallGuy | _: BombPod | _: CreepingShadow => true
    case _ => false
  }

  def ground(p: Painter): Unit = {
    import p.*

    for shadow <- gameState.allTEntities[CreepingShadow].values do {
      val touchesBoss = gameState.bosses.values.exists(shadow.collides(_, now))
      val edge =
        if touchesBoss then (4.0, touchingColour.withAlpha(0.6 + 0.3 * math.sin(now / 120.0)))
        else (2.0, shadowColour.withAlpha(0.8))
      circle(position(shadow), shadow.radius, Some(shadowColour.withAlpha(0.35)), Some(edge))
    }

    for bomb <- gameState.allTEntities[BombPod].values do {
      val colour    = if ExplodeBombs.isCovered(gameState, bomb, now) then coveredColour else uncoveredColour
      val remaining = 1.0 - (now - bomb.time).toDouble / timeBeforeExplosion
      val radius    = BombPod.shape.radius
      circle(bomb.pos, radius, Some(colour.withAlpha(0.6)), Some((2, colour)))
      circle(
        bomb.pos,
        radius + math.max(0.0, remaining) * 3 * radius,
        None,
        Some((2, colour.withAlpha(0.8)))
      )
    }
  }

  def adds(p: Painter): Unit = {
    for smallGuy <- p.gameState.allTEntities[SmallGuy].values do {
      p.shape(smallGuy, Some(smallGuyColour.withAlpha(0.3)), Some((1, smallGuyColour)))
      withBars(p, smallGuy)
    }
    for bigGuy <- p.gameState.allTEntities[BigGuy].values do {
      p.shape(bigGuy, Some(addColour.withAlpha(0.3)), Some((2, addColour)))
      withBars(p, bigGuy)
    }
  }

  /** The more broken armours, the more damage the player takes: the tint gets stronger. */
  override def playerFrameTint(gameState: GameState, playerId: Entity.Id): Option[(Paint, Double)] = {
    val stacks = gameState.allBuffsOfEntity(playerId).count(_.isInstanceOf[BrokenArmor])
    Option.when(stacks > 0)((brokenArmorColour, math.min(0.75, 0.25 * stacks)))
  }

  override def hudText(gameState: GameState): Option[String] = {
    val bombs = gameState.allTEntities[BombPod].values
    val bombsText =
      if bombs.isEmpty then ""
      else
        s" · Bombs covered: ${bombs.count(ExplodeBombs.isCovered(gameState, _, gameState.time))}" ++
          s"/${bombs.size}"
    Some(
      s"Small guys: ${gameState.allTEntities[SmallGuy].size} · " ++
        s"Big guys: ${gameState.allTEntities[BigGuy].size}$bombsText"
    )
  }

  /** The bombs explode when the boss is done casting [[ExplodeBombs]], which comes off cooldown
    * [[ExplodeBombs.cooldown]] after they are placed.
    */
  private inline def timeBeforeExplosion: Double =
    (ExplodeBombs.cooldown + ExplodeBombs.castingTime).toDouble
}
