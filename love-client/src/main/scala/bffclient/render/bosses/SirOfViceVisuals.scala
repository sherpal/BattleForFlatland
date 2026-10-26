package bffclient.render.bosses

import bffclient.render.Painter
import bffclient.ui.Colour as Paint
import gamelogic.abilities.boss.sirofvice.{CleansingNova, HolyWrath, SacredGround}
import gamelogic.entities.Entity
import gamelogic.entities.boss.firstraid.SirOfVice
import gamelogic.entities.boss.sirofvice.HolyWrathChain

/** Sir of Vice: its Sacred Ground aura, the Cleansing Nova growing until it reaches the walls, the
  * zone around the target of the Holy Wrath, and then (briefly) the path of the wrath.
  */
object SirOfViceVisuals extends BossVisuals {
  val bossName: String = SirOfVice.name

  private val auraColour  = Paint.rgb(255, 200, 60)
  private val novaColour  = Paint(1, 1, 1)
  private val wrathColour = Paint.rgb(255, 240, 150)

  def draws(entity: Entity): Boolean = entity.isInstanceOf[HolyWrathChain]

  def ground(p: Painter): Unit = {
    import p.*
    for case boss: SirOfVice <- gameState.bosses.values do {
      val centre = position(boss)
      circle(
        centre,
        SacredGround.radius,
        Some(auraColour.withAlpha(0.08)),
        Some((2, auraColour.withAlpha(0.5)))
      )
      for info <- casting(p, boss.id) do info.ability match {
        case _: CleansingNova =>
          circle(
            centre,
            math.max(1.0, progress(info, now) * SirOfVice.roomRadius),
            Some(novaColour.withAlpha(0.1)),
            Some((3, novaColour.withAlpha(0.6)))
          )
        case wrath: HolyWrath =>
          for target <- gameState.players.get(wrath.targetId) do
            circle(
              position(target),
              HolyWrath.jumpRange,
              Some(wrathColour.withAlpha(0.15)),
              Some((2, wrathColour.withAlpha(0.8)))
            )
        case _ => ()
      }
    }
    for
      chain <- gameState.allTEntities[HolyWrathChain].values
      if gameState.time - chain.time < HolyWrathChain.displayDuration
    do {
      val alpha = 1.0 - (gameState.time - chain.time).toDouble / HolyWrathChain.displayDuration
      for (from, to) <- chain.positions.zip(chain.positions.drop(1)) do
        line(from, to, 4, wrathColour.withAlpha(alpha))
    }
  }

  /** Sir of Vice has no adds. */
  def adds(p: Painter): Unit = ()
}
