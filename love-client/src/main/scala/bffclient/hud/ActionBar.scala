package bffclient.hud

import scala.collection.mutable

import bffclient.game.GameClient
import bffclient.hud.HudStyle.*
import bffclient.love.Graphics
import bffclient.render.Sprites
import bffclient.ui.{Colour as Paint, Rect}
import gamelogic.abilities.Ability
import gamelogic.docs.AbilityDoc
import gamelogic.entities.classes.PlayerClass
import gamelogic.gamestate.GameState

/** Our abilities, as icons in frames (like the frontend's): pushed down just after it is used,
  * darkened from the top during its cooldown with the remaining seconds, the key in a corner, and
  * the colour of the ability as a line at the bottom. Hovering an ability shows what it does
  * (`AbilityDoc`, like in the lobby's documentation).
  *
  * @param mouseOver
  *   whether the mouse is over this rectangle (of the HUD, which is drawn at its scale)
  */
final class ActionBar(client: GameClient, mouseOver: Rect => Boolean) {
  private val slotSize = 52.0
  private val gap      = 6.0
  private val iconSize = 40.0

  private val docsCache = mutable.Map.empty[String, Map[Ability.AbilityId, AbilityDoc]]

  private def docs(player: PlayerClass): Map[Ability.AbilityId, AbilityDoc] =
    docsCache.getOrElseUpdate(
      player.cls.value,
      player.cls.builder.abilitiesDoc.map(doc => doc.abilityId -> doc).toMap
    )

  /** Where the bar is (above the bottom of the window). */
  def top(height: Double): Double = height - slotSize - 8

  def draw(gameState: GameState, now: Long, width: Double, height: Double): Unit =
    for
      id <- client.myId
      me <- gameState.players.get(id)
    do {
      val abilities = me.abilities.toVector
      val left      = (width - abilities.length * (slotSize + gap) + gap) / 2
      val slots     = abilities.zipWithIndex.map { (abilityId, i) =>
        abilityId -> Rect(left + i * (slotSize + gap), top(height), slotSize, slotSize)
      }
      for ((abilityId, slot), i) <- slots.zipWithIndex do
        drawSlot(me, abilityId, i, slot, now)
      // Over everything else of the HUD
      for
        ((abilityId, slot), i) <- slots.zipWithIndex.find((slot, _) => mouseOver(slot._2))
        doc                    <- docs(me).get(abilityId)
      do tooltip(doc, i, slot, width)
    }

  private def drawSlot(me: PlayerClass, abilityId: Ability.AbilityId, index: Int, slot: Rect, now: Long): Unit = {
    val used     = me.relevantUsedAbilities.get(abilityId)
    val cooldown = for
      u         <- used
      remaining <- Some(u.time + u.cooldown - now).filter(_ > 0)
    yield (remaining, u.cooldown)
    val pushed = if used.exists(u => now - u.time < 100) then 2.0 else 0.0

    panel(slot, radius = 8)
    if mouseOver(slot) then roundedLine(slot, 8, Paint(1, 1, 1, 0.5), 1)
    val icon = Rect(
      slot.x + (slot.width - iconSize) / 2,
      slot.y + (slot.height - iconSize) / 2 - 2 + pushed,
      iconSize,
      iconSize
    )
    Sprites.abilityIcons.get(abilityId) match {
      case Some(sprite) =>
        Graphics.setColor(1, 1, 1)
        sprite.draw(icon.x + iconSize / 2, icon.y + iconSize / 2, 0, iconSize, iconSize)
      case None =>
        roundedFill(icon, 4, Paint(0, 0, 0, 0.3))
        val initials = docs(me).get(abilityId).fold(s"#$abilityId")(_.name.split(' ').map(_.take(1)).mkString)
        text(initials, icon, "center")
    }
    for (remaining, total) <- cooldown do {
      Graphics.setColor(0, 0, 0, 0.6)
      Graphics.rectangle("fill", icon.x, icon.y, iconSize, iconSize * remaining / total.toDouble)
      text(f"${remaining / 1000.0}%.1f", icon, "center", textSize = Some(titleSize), padding = 0)
    }
    // A thin dark border, so that the (often white) icons do not glare on the panels
    roundedLine(Rect(icon.x - 1, icon.y - 1, iconSize + 2, iconSize + 2), 3, Paint(0, 0, 0, 0.85), 2)
    // The colour of the ability, as a line at the bottom
    roundedFill(
      Rect(slot.x + 8, slot.bottom - 4, slot.width - 16, 2),
      1,
      softened(Ability.abilityColour(abilityId))
    )
    // The key, in the bottom right corner of the icon
    val key   = shortLabel(client.controls.abilityKeys.lift(index).fold("-")(_.label))
    val badge = Rect(icon.right - 4 - 6 * key.length, icon.bottom - 11, 6 + 6 * key.length, 13)
    roundedFill(badge, 3, Paint(0, 0, 0, 0.7))
    text(key, badge, "center", padding = 0, textSize = Some(smallSize))
  }

  /** Short enough for the corner of an icon: Digit1 is 1, KeyQ is Q, Shift KeyQ is S-Q, Button 2
    * (of the mouse) is M2.
    */
  private def shortLabel(label: String): String =
    label.replace("Shift ", "S-").replace("Digit", "").replace("Key", "").replace("Button ", "M")

  /** Name and key, cast time, cooldown and cost, and what it does, above the ability. */
  private def tooltip(doc: AbilityDoc, index: Int, slot: Rect, width: Double): Unit = {
    val tooltipWidth = 300.0
    val padding      = 10.0
    val inner        = tooltipWidth - 2 * padding
    val details      = (Seq(s"Cast: ${doc.castingTimeRepr}", s"Cooldown: ${doc.cooldownRepr}") ++
      doc.maybeCostRepr.map(cost => s"Cost: $cost")).mkString("  ·  ")
    val detailLines      = wrap(details, inner, smallSize)
    val descriptionLines = wrap(doc.description, inner)
    val titleHeight      = lineHeight(titleSize)
    val height = padding + titleHeight + 6 + detailLines.size * lineHeight(smallSize) + 6 +
      descriptionLines.size * lineHeight() + padding
    val x    = math.max(4.0, math.min(width - tooltipWidth - 4, slot.x + slot.width / 2 - tooltipWidth / 2))
    val rect = Rect(x, slot.y - 8 - height, tooltipWidth, height)
    roundedFill(rect, 8, Paint.rgb(30, 34, 46).withAlpha(0.95))
    roundedLine(rect, 8, softened(Ability.abilityColour(doc.abilityId)), 1)

    var y = rect.y + padding
    text(doc.name, Rect(rect.x + padding, y, inner, titleHeight), textSize = Some(titleSize), padding = 0)
    val key = client.controls.abilityKeys.lift(index).fold("")(_.label)
    text(key, Rect(rect.x + padding, y, inner, titleHeight), "right", mutedColour, padding = 0)
    y += titleHeight + 6
    for line <- detailLines do {
      text(line, Rect(rect.x + padding, y, inner, lineHeight(smallSize)), colour = mutedColour, padding = 0,
        textSize = Some(smallSize))
      y += lineHeight(smallSize)
    }
    y += 6
    for line <- descriptionLines do {
      text(line, Rect(rect.x + padding, y, inner, lineHeight()), padding = 0)
      y += lineHeight()
    }
  }
}
