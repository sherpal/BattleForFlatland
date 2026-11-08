package bffclient.render.bosses

import bffclient.render.Painter
import bffclient.ui.Colour as Paint
import gamelogic.abilities.boss.spiritofnara.*
import gamelogic.buffs.boss.spiritofnara.{Chained, Drinking, FlameWreathWatch, Polymorphed}
import gamelogic.entities.Entity
import gamelogic.entities.boss.firstraid.SpiritOfNara
import gamelogic.entities.boss.spiritofnara.{BlizzardCloud, FlameWreathRing, WaterElemental}
import gamelogic.gamestate.GameState

/** The Spirit of Nara: the reach of its Counterspell, the spell it casts (a line to its target, in
  * the colour of the school), the Arcane Explosion, the rings of the Flame Wreath, the Blizzard and
  * the Water Elementals. Polymorphed and chained players are marked on the ground and on their
  * frames.
  */
object SpiritOfNaraVisuals extends BossVisuals {
  val bossName: String = SpiritOfNara.name

  private val arcaneColour    = Paint.rgb(190, 90, 255)
  private val fireColour      = Paint.rgb(255, 120, 20)
  private val frostColour     = Paint.rgb(120, 210, 255)
  private val waterColour     = Paint.rgb(40, 110, 230)
  private val polymorphColour = Paint.rgb(255, 150, 210)
  private val drinkColour     = Paint.rgb(80, 140, 255)

  def draws(entity: Entity): Boolean = entity match {
    case _: WaterElemental | _: FlameWreathRing | _: BlizzardCloud => true
    case _ => false
  }

  private def schoolColour(school: Int): Paint = school match {
    case MagicSchool.fire => fireColour
    case MagicSchool.frost => frostColour
    case _ => arcaneColour
  }

  def ground(p: Painter): Unit = {
    import p.*
    val time = now.toDouble

    for case boss: SpiritOfNara <- gameState.bosses.values do {
      val centre = position(boss)

      // the reach of the Counterspell, which the casters should stay out of
      circle(centre, Counterspell.radius, None, Some((1, arcaneColour.withAlpha(0.35))))

      // drinking: a pulsing ring of water
      if boss.isDrinking(gameState) then
        circle(
          centre,
          boss.shape.radius + 8 + 3 * math.sin(time / 150.0),
          None,
          Some((4, drinkColour.withAlpha(0.8)))
        )

      // the Arcane Explosion: its whole reach as soon as players are pulled, filling up while cast
      val explosionProgress = casting(p, boss.id).collect {
        case info if info.ability.isInstanceOf[ArcaneExplosion] => progress(info, now)
      }
      if explosionProgress.isDefined || boss.arcaneExplosionPending then {
        circle(
          centre,
          ArcaneExplosion.radius,
          Some(arcaneColour.withAlpha(0.08)),
          Some((3, arcaneColour.withAlpha(0.5 + 0.4 * math.abs(math.sin(time / 120.0)))))
        )
        for progression <- explosionProgress do
          circle(
            centre,
            math.max(1.0, progression * ArcaneExplosion.radius),
            Some(arcaneColour.withAlpha(0.25))
          )
      }

      for info <- casting(p, boss.id) do
        info.ability match {
          // the spell flies towards its target
          case nuke: NaraNuke =>
            for target <- gameState.players.get(nuke.targetId) do {
              val colour = schoolColour(nuke.school)
              val from   = centre
              val to     = position(target)
              line(from, to, 2, colour.withAlpha(0.35))
              circle(from + (to - from) * progress(info, now), 6, Some(colour))
            }
          case _: Pyroblast =>
            circle(
              centre,
              boss.shape.radius + 10 + 30 * progress(info, now),
              Some(fireColour.withAlpha(0.2)),
              Some((3, fireColour))
            )
          case _ => ()
        }
    }

    // the Flame Wreath: flickering rings, each of its insiders tied to it
    val watch = gameState.allTBuffs[FlameWreathWatch].headOption
    for ring <- gameState.allTEntities[FlameWreathRing].values do {
      val remaining = watch.fold(0.0)(_.remainingPercentage(gameState.time))
      circle(
        ring.pos,
        FlameWreathRing.radius,
        Some(fireColour.withAlpha(0.06 + 0.04 * math.sin(time / 90.0))),
        Some((4 + 2 * math.sin(time / 70.0), fireColour.withAlpha(0.5 + 0.4 * remaining)))
      )
      circle(
        ring.pos,
        FlameWreathRing.radius - 5,
        None,
        Some((2, Paint.rgb(255, 220, 80).withAlpha(0.6)))
      )
    }

    // the Blizzard: a cold storm, with the path it follows
    for cloud <- gameState.allTEntities[BlizzardCloud].values do {
      val cloudCentre = cloud.centreAt(now)
      circle(
        cloudCentre,
        BlizzardCloud.radius,
        Some(frostColour.withAlpha(0.25)),
        Some((3, frostColour.withAlpha(0.85)))
      )
      for k <- 0 until 6 do {
        val angle = time / 300.0 + k * math.Pi / 3
        val flake = cloudCentre +
          gamelogic.physics.Complex.polar(
            BlizzardCloud.radius * 0.6,
            gamelogic.entities.WithPosition.Angle.fromDouble(angle)
          )
        circle(flake, 4, Some(Paint(1, 1, 1).withAlpha(0.8)))
      }
    }

    // polymorphed and chained players
    for
      buff   <- gameState.allTBuffs[Polymorphed]
      player <- gameState.players.get(buff.bearerId)
    do
      circle(
        position(player),
        24,
        Some(polymorphColour.withAlpha(0.45)),
        Some((2, polymorphColour))
      )
    for
      buff   <- gameState.allTBuffs[Chained]
      player <- gameState.players.get(buff.bearerId)
    do circle(position(player), 22, None, Some((4, frostColour)))
  }

  /** The Water Elementals, with their Waterbolts flying to their targets. */
  def adds(p: Painter): Unit = {
    import p.*
    for elemental <- gameState.allTEntities[WaterElemental].values do {
      for
        info <- casting(p, elemental.id)
        case bolt: Waterbolt <- Some(info.ability)
        target <- gameState.players.get(bolt.targetId)
      do {
        val from = position(elemental)
        val to   = position(target)
        line(from, to, 2, waterColour.withAlpha(0.3))
        circle(from + (to - from) * progress(info, now), 5, Some(waterColour))
      }
      shape(elemental, Some(waterColour.withAlpha(0.6)), Some((2, frostColour)))
      withBars(p, elemental)
    }
  }

  /** Polymorphed players can do nothing, chained players can't move (and can be dispelled). */
  override def playerFrameTint(gameState: GameState, playerId: Entity.Id): Option[(Paint, Double)] =
    gameState.allBuffsOfEntity(playerId).collectFirst {
      case _: Polymorphed => (polymorphColour, 0.4)
      case _: Chained => (frostColour, 0.4)
    }

  /** The mana of the boss decides when it drinks. */
  override def hudText(gameState: GameState): Option[String] =
    gameState.bosses.values.collectFirst { case boss: SpiritOfNara =>
      val mana = math.round(100 * boss.resourceAmount.amount / SpiritOfNara.maxMana)
      if gameState.hasBuffOfType[Drinking](boss.id) then s"Mana: $mana% (drinking)"
      else s"Mana: $mana%"
    }
}
