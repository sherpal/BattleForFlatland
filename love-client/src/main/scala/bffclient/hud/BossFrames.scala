package bffclient.hud

import scala.collection.mutable

import bffclient.game.GameClient
import bffclient.gui.*
import bffclient.hud.HudStyle.*
import bffclient.hud.HudWidgets.*
import bffclient.render.Painter
import bffclient.ui.Colour as Paint
import gamelogic.abilities.Ability
import gamelogic.entities.Entity
import utils.misc.RGBColour

/** Life and name of the boss, what it casts, and its buffs, on top (clicking it targets it). */
final class BossFrame(parent: Frame, client: GameClient) extends UnitButton {
  var unit = Option.empty[Entity.Id]

  val frame: Frame = Frame(parent)
  frame.setBackdrop(panelBackdrop)
  frame.setWidth(300)
  frame.setPoint(Point.Top, parent, Point.Top, 0, 4)
  frame.enableMouse(true)
  frame.registerForClicks(1)
  frame.setScript(Script.OnClick)(_ => unit.foreach(client.selectTarget))

  private val life = HudBar(frame, 290, 18)
  life.setPoint(Point.TopLeft, frame, Point.TopLeft, 5, 5)
  life.label.setJustifyH(JustifyH.Center)
  private val castBar = HudBar(frame, 290, 13)
  castBar.setPoint(Point.TopLeft, frame, Point.TopLeft, 5, 26)
  castBar.label.setJustifyH(JustifyH.Center)
  private val buffs = BuffRow(frame, 14, 16, 18)
  // Lightens the frame under the mouse, over its bars
  private val overlay = Frame(frame)
  overlay.setAllPoints()
  overlay.setFrameLevel(frame.frameLevel + 5)
  private val highlight = overlay.createTexture(Paint(1, 1, 1, 0.1), Layer.Highlight)
  highlight.setAllPoints(overlay)
  highlight.setCornerRadius(6)
  highlight.setOutline(Some(Paint(1, 1, 1, 0.5)))

  def update(p: Painter): Unit = {
    val boss = p.gameState.bosses.values.headOption
    unit = boss.map(_.id)
    frame.setShown(boss.isDefined)
    for b <- boss do {
      val lifeRatio = ratio(b.life, b.maxLife)
      life.setValue(lifeRatio)
      life.setColour(lifeColour(lifeRatio))
      life.label.setText(b.name)
      life.value.setText(s"${math.round(lifeRatio * 100)}%")

      val casting = p.gameState.castingEntityInfo.get(b.id).filter(_.isCasting(p.now, 0L))
      castBar.setShown(casting.isDefined)
      for info <- casting do {
        castBar.setValue(Hud.castProgress(p, info))
        castBar.setColour(softened(info.ability.abilityColour))
        castBar.label.setText(Hud.abilityName(b, info.ability.abilityId))
      }
      val iconBuffs = p.iconBuffs(b.id)
      buffs.update(iconBuffs, p.gameState.time)
      val buffsTop = if casting.isDefined then 42.0 else 26.0
      if buffs.frame.points.headOption.forall(_.y != buffsTop) then
        buffs.frame.setPoint(Point.TopLeft, frame, Point.TopLeft, 6, buffsTop)
      frame.setHeight(buffsTop + (if iconBuffs.nonEmpty then 19 else 2))
      highlight.setShown(frame.gui.isHovered(frame))
    }
  }
}

/** A panel of the right side, with a title and rows of bars (the boss's cooldowns, the threat). */
private abstract class BarsPanel(parent: Frame, title: String) {
  val frame: Frame = panel(parent)
  frame.setWidth(164)
  private val titleText = text(frame, colour = mutedColour)
  titleText.setText(title)
  titleText.setPoint(Point.TopLeft, frame, Point.TopLeft, 6, 3)
  titleText.setPoint(Point.TopRight, frame, Point.TopRight, -6, 3)
  titleText.setHeight(16)

  private val rows = mutable.ArrayBuffer.empty[HudBar]

  /** Shows `count` rows (and sizes the panel for them), each set by `show`. */
  protected def showRows(count: Int)(show: (HudBar, Int) => Unit): Unit = {
    while rows.size < count do {
      val row = HudBar(frame, 152, 14)
      row.setPoint(Point.TopLeft, frame, Point.TopLeft, 6, 21 + 17 * rows.size)
      rows += row
    }
    for (row, i) <- rows.zipWithIndex do {
      row.setShown(i < count)
      if i < count then show(row, i)
    }
    frame.setHeight(24 + 17 * count)
  }
}

/** One bar per ability of the boss, in its colour, emptying until it is ready again (top right). */
final class BossCooldowns(parent: Frame) extends BarsPanel(parent, "Boss abilities") {
  frame.setPoint(Point.TopRight, parent, Point.TopRight, -4, 4)

  def update(p: Painter): Unit = {
    val boss = p.gameState.bosses.values.headOption
    frame.setShown(boss.isDefined)
    for b <- boss do {
      val abilities = b.abilityNames.toVector
      showRows(abilities.size) { (row, i) =>
        val (abilityId, name) = abilities(i)
        val lastUse           = b.relevantUsedAbilities.get(abilityId)
        val remaining = lastUse.fold(0L)(use => math.max(0L, use.cooldown - (p.now - use.time)))
        row.setValue(ratio(remaining.toDouble, lastUse.fold(1L)(_.cooldown).toDouble))
        row.setColour(softened(Ability.abilityColour(abilityId)))
        row.label.setText(name)
        row.value.setText(
          if remaining <= 0 then ""
          else if remaining >= 1000 then s"${remaining / 1000}s"
          else f"${remaining / 1000.0}%.1f"
        )
      }
    }
  }
}

/** The players by the boss's threat towards them, the highest first (bottom right). */
final class ThreatMeter(parent: Frame) extends BarsPanel(parent, "Threat") {
  frame.setPoint(Point.BottomRight, parent, Point.BottomRight, -4, -8)

  def update(p: Painter): Unit = {
    val boss = p.gameState.bosses.values.headOption
    frame.setShown(boss.isDefined)
    for b <- boss do {
      val threats = b.damageThreats.toVector
        .flatMap((id, amount) => p.gameState.players.get(id).map(_ -> amount.value))
        .sortBy(-_._2)
      val maxThreat = threats.headOption.map(_._2).filter(_ > 0).getOrElse(1.0)
      showRows(threats.size) { (row, i) =>
        val (player, amount) = threats(i)
        row.setValue(amount / maxThreat)
        row.setColour(softened(RGBColour.fromIntColour(player.colour)))
        row.label.setText(player.name)
      }
    }
  }
}
