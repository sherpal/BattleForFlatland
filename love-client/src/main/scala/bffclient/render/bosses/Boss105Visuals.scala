package bffclient.render.bosses

import bffclient.render.Painter
import bffclient.ui.Colour as Paint
import gamelogic.abilities.boss.boss105.{Resonance, Reunion, Siphon, Skewer}
import gamelogic.buffs.boss.boss105.{Mourning, TwinBond, VolatileCharge, Yearning}
import gamelogic.entities.Entity
import gamelogic.entities.boss.boss105.{ReformationPoint, Shade}
import gamelogic.entities.boss.dawnoftime.Boss105
import gamelogic.gamestate.GameState

/** Boss 105: its Shade, the twins' auras and bond, its ring, the Skewer's beam, the Reunion, the
  * volatile charges, the reformation points and the Siphon.
  */
object Boss105Visuals extends BossVisuals {
  val bossName: String = Boss105.name

  private val bondColour      = Paint.rgb(80, 220, 240)
  private val exposedColour   = Paint.rgb(255, 150, 30)
  private val shadeColour     = Paint.rgb(90, 40, 140)
  private val resonanceColour = Paint.rgb(230, 30, 60)
  private val beamColour      = Paint.rgb(255, 60, 60)
  private val chargeColour    = Paint.rgb(255, 110, 20)
  private val siphonColour    = Paint.rgb(120, 220, 90)

  def draws(entity: Entity): Boolean = entity match {
    case _: Shade | _: ReformationPoint => true
    case _                              => false
  }

  def ground(p: Painter): Unit = {
    import p.*
    val time = now.toDouble

    for case boss: Boss105 <- gameState.bosses.values do {
      val bossPosition = position(boss)

      // The twins' auras (half the resonance range) touch exactly when they resonate
      for shade <- gameState.allTEntities[Shade].values do {
        val shadePosition = position(shade)
        val resonating    = bossPosition.distanceTo(shadePosition) < Resonance.range
        val colour        = if resonating then resonanceColour else shadeColour
        val pulse         = if resonating then 0.12 + 0.08 * math.sin(time / 80.0) else 0.0
        for centre <- Seq(bossPosition, shadePosition) do
          circle(
            centre,
            Resonance.range / 2,
            Some(colour.withAlpha(0.05 + pulse)),
            Some((2, colour.withAlpha(if resonating then 0.9 else 0.35)))
          )
        line(
          bossPosition,
          shadePosition,
          if resonating then 5 else 2,
          colour.withAlpha(if resonating then 0.9 else 0.3)
        )
      }

      // Ring: cyan while bonded to its Shade, orange and pulsing when exposed
      val ringRadius = boss.shape.radius + 6
      if gameState.hasBuffOfType[Mourning](boss.id) then
        circle(
          bossPosition,
          ringRadius + 3 * math.sin(time / 120.0),
          Some(exposedColour.withAlpha(0.2)),
          Some((5, exposedColour))
        )
      else if gameState.hasBuffOfType[TwinBond](boss.id) then
        circle(bossPosition, ringRadius, None, Some((4, bondColour.withAlpha(0.9))))

      // The Skewer's beam, from the boss through its target up to the wall, while it is cast
      for
        info <- casting(p, boss.id)
        case skewer: Skewer <- Some(info.ability)
        target <- gameState.players.get(skewer.targetId)
        direction = (position(target) - bossPosition).safeNormalized
        if direction.modulus2 > 0
      do {
        def toWall(from: Double, speed: Double) =
          if speed > 1e-9 then (Boss105.size - from) / speed
          else if speed < -1e-9 then (-Boss105.size - from) / speed
          else Double.MaxValue
        val length = math.max(
          0.0,
          math.min(toWall(bossPosition.re, direction.re), toWall(bossPosition.im, direction.im))
        )
        val side = direction.orthogonal * Skewer.halfWidth
        val end  = bossPosition + direction * length
        polygon(
          Seq(bossPosition - side, end - side, end + side, bossPosition + side),
          Some(beamColour.withAlpha(0.12 + 0.35 * progress(info, now))),
          Some((2, beamColour.withAlpha(0.8)))
        )
      }

      // The Reunion: while it is cast, and while the Shade is on its way
      val reunionAlpha = casting(p, boss.id)
        .filter(_.ability.isInstanceOf[Reunion])
        .map(info => 0.2 + 0.5 * progress(info, now))
        .orElse(
          Option.when(
            gameState.allTEntities[Shade].keys.exists(gameState.hasBuffOfType[Yearning](_))
          )(0.7)
        )
      for
        alpha <- reunionAlpha
        shade <- gameState.allTEntities[Shade].values.headOption
      do
        line(bossPosition, position(shade), 6, shadeColour.withAlpha(alpha))
    }

    // The explosion radius of the charges, filling up until they explode
    for
      charge <- gameState.allTBuffs[VolatileCharge]
      bearer <- gameState.players.get(charge.bearerId)
    do {
      val elapsed = chargeElapsed(gameState, charge)
      circle(
        position(bearer),
        VolatileCharge.radius,
        Some(chargeColour.withAlpha(0.06)),
        Some((2, chargeColour.withAlpha(0.8)))
      )
      circle(
        position(bearer),
        math.max(1.0, VolatileCharge.radius * elapsed),
        Some(chargeColour.withAlpha(0.3))
      )
    }

    // Where the Shade comes back, more and more solid until it is back
    for point <- gameState.allTEntities[ReformationPoint].values do {
      val elapsed = gameState
        .allTBuffs[Mourning]
        .headOption
        .fold(0.0)(m =>
          math.min(1.0, (gameState.time - m.appearanceTime).toDouble / Mourning.duration)
        )
      circle(
        point.pos,
        ReformationPoint.radius,
        Some(shadeColour.withAlpha(0.1 + 0.3 * elapsed)),
        Some((2, shadeColour.withAlpha(0.8)))
      )
      polygon(
        Shade.shape.vertices.map(point.pos + _),
        Some(shadeColour.withAlpha(0.15 + 0.4 * elapsed)),
        Some((2, shadeColour))
      )
    }
  }

  /** The Shade (outlined in white when it yearns for the boss), and the drain of its Siphon. */
  def adds(p: Painter): Unit = {
    import p.*
    for shade <- gameState.allTEntities[Shade].values do {
      val yearning = gameState.hasBuffOfType[Yearning](shade.id)
      for
        info <- casting(p, shade.id)
        case siphon: Siphon <- Some(info.ability)
        target <- gameState.players.get(siphon.targetId)
      do line(
        position(shade),
        position(target),
        3,
        siphonColour.withAlpha(0.2 + 0.6 * progress(info, now))
      )
      shape(
        shade,
        Some(shadeColour.withAlpha(0.55)),
        Some((if yearning then 4 else 2, if yearning then Paint(1, 1, 1) else Paint(0, 0, 0)))
      )
      withBars(p, shade)
    }
  }

  /** The bearer of a volatile charge must be dispelled or go away from everybody (more and more
    * urgently).
    */
  override def playerFrameTint(gameState: GameState, playerId: Entity.Id): Option[(Paint, Double)] =
    gameState.allBuffsOfEntity(playerId).collectFirst { case charge: VolatileCharge =>
      (chargeColour, 0.25 + 0.5 * chargeElapsed(gameState, charge))
    }

  /** From 0 when the charge appears, to 1 when it explodes. */
  private def chargeElapsed(gameState: GameState, charge: VolatileCharge): Double =
    math.min(
      1.0,
      math.max(0.0, (gameState.time - charge.appearanceTime).toDouble / VolatileCharge.duration)
    )
}
