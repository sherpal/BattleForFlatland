package bffclient.hud

import scala.collection.mutable

import bffclient.controls.ClientSettings
import bffclient.gui.*
import bffclient.hud.HudStyle.*
import bffclient.hud.HudWidgets.*
import bffclient.love.LoveInput
import bffclient.render.{Painter, Sprites}
import bffclient.ui.Colour as Paint
import gamelogic.abilities.Ability
import gamelogic.docs.AbilityDoc
import gamelogic.entities.classes.PlayerClass
import services.i18n.I18n

/** Our abilities, as icons in frames (like the frontend's), centred at the bottom in the order
  * chosen in the settings (`ClientSettings.abilityOrder`): pushed down just after it is used,
  * darkened from the top during its cooldown with the remaining seconds, the key in a corner, and
  * the colour of the ability as a line at the bottom. Hovering an ability shows what it does
  * (`AbilityDoc`, like in the lobby's documentation) in a tooltip.
  */
final class ActionBar(parent: Frame, client: HudClient) {
  import ActionBar.*

  val frame: Frame = Frame(parent)
  frame.setHeight(slotSize)
  frame.setPoint(Point.Bottom, parent, Point.Bottom, 0, -8)

  private val docsCache = mutable.Map.empty[String, Map[Ability.AbilityId, AbilityDoc]]

  private def docs(player: PlayerClass): Map[Ability.AbilityId, AbilityDoc] =
    docsCache.getOrElseUpdate(
      player.cls.value,
      player.cls.builder.abilitiesDoc.map(doc => doc.abilityId -> doc).toMap
    )

  private val tooltip = Tooltip(parent, client.i18n)
  private val slots   = mutable.ArrayBuffer.empty[Slot]

  private final class Slot(index: Int) {
    var shown = Option.empty[(Ability.AbilityId, Option[AbilityDoc])]

    val frame: Frame = panel(ActionBar.this.frame, radius = 8)
    frame.setSize(slotSize, slotSize)
    frame.setPoint(Point.TopLeft, ActionBar.this.frame, Point.TopLeft, index * (slotSize + gap), 0)
    frame.enableMouse(true)
    frame.registerForClicks(1)
    frame.setScript(Script.OnEnter)(() =>
      for
        (_, doc) <- shown
        d        <- doc
      do tooltip.show(d, keyLabel(index), frame)
    )
    frame.setScript(Script.OnLeave)(() => tooltip.hide())

    private val hoverRing = frame.createTexture(Paint(0, 0, 0, 0), Layer.Highlight)
    hoverRing.setAllPoints(frame)
    hoverRing.setCornerRadius(8)
    hoverRing.setOutline(Some(Paint(1, 1, 1, 0.5)))

    private val icon = frame.createTexture(Layer.Artwork)
    icon.setSize(iconSize, iconSize)
    private var pushedBy = -1.0
    private val initials = textOver(frame, icon, JustifyH.Center, padding = 0)
    private val shade    = frame.createTexture(Paint(0, 0, 0, 0.6), Layer.Overlay)
    shade.setPoint(Point.TopLeft, icon, Point.TopLeft)
    private var shadeAt = -1.0
    private val cooldown =
      textOver(frame, icon, JustifyH.Center, size = titleSize, padding = 0)
    // A thin dark border, so that the (often white) icons do not glare on the panels
    private val border = frame.createTexture(Paint(0, 0, 0, 0), Layer.Overlay)
    border.setPoint(Point.TopLeft, icon, Point.TopLeft, -1, -1)
    border.setPoint(Point.BottomRight, icon, Point.BottomRight, 1, 1)
    border.setCornerRadius(3)
    border.setOutline(Some(Paint(0, 0, 0, 0.85)), 2)
    // The colour of the ability, as a line at the bottom
    private val colourLine = frame.createTexture(Layer.Artwork)
    place(colourLine, frame, 8, slotSize - 4, slotSize - 16, 2)
    colourLine.setCornerRadius(1)
    // The key, in the bottom right corner of the icon
    private val badge = frame.createTexture(Paint(0, 0, 0, 0.7), Layer.Overlay)
    badge.setPoint(Point.BottomRight, icon, Point.BottomRight, 2, 2)
    badge.setHeight(13)
    badge.setCornerRadius(3)
    private val key = textOver(frame, badge, JustifyH.Center, size = smallSize, padding = 0)

    def update(me: PlayerClass, abilityId: Option[Ability.AbilityId], now: Long): Unit = {
      frame.setShown(abilityId.isDefined)
      for id <- abilityId do {
        if !shown.exists(_._1 == id) then {
          shown = Some((id, docs(me).get(id)))
          Sprites.abilityIcons.get(id) match {
            case Some(sprite) =>
              setSprite(icon, sprite)
              icon.setColour(Paint(1, 1, 1))
              icon.setCornerRadius(0)
              initials.setText("")
            case None =>
              icon.clearImage()
              icon.setColour(Paint(0, 0, 0, 0.3))
              icon.setCornerRadius(4)
              initials.setText(
                docs(me).get(id).fold(s"#$id")(_.name.split(' ').map(_.take(1)).mkString)
              )
          }
          colourLine.setColour(softened(Ability.abilityColour(id)))
        }

        val used   = me.relevantUsedAbilities.get(id)
        val pushed = if used.exists(u => now - u.time < 100) then 2.0 else 0.0
        if pushed != pushedBy then {
          pushedBy = pushed
          icon.setPoint(Point.Center, frame, Point.Center, 0, -2 + pushed)
        }
        val remaining = for
          u    <- used
          left <- Some(u.time + u.cooldown - now).filter(_ > 0)
        yield (left, u.cooldown)
        shade.setShown(remaining.isDefined)
        val fraction = remaining.fold(0.0)((left, total) => left / total.toDouble)
        if fraction != shadeAt then {
          shadeAt = fraction
          shade.setPoint(Point.BottomRight, icon, Point(1, fraction))
        }
        cooldown.setText(remaining.fold("")((left, _) => f"${left / 1000.0}%.1f"))

        val label = shortLabel(keyLabel(index).getOrElse("-"))
        key.setText(label)
        badge.setWidth(6 + 6 * label.length)
        hoverRing.setShown(frame.gui.isHovered(frame))
      }
    }
  }

  private def keyLabel(index: Int): Option[String] =
    client.controls.abilityKeys.lift(index).map(LoveInput.label)

  def update(p: Painter): Unit = {
    val me = client.myId.flatMap(p.gameState.players.get)
    frame.setShown(me.isDefined)
    for player <- me do {
      val abilities = ClientSettings.current.abilityOrder(player.cls)
      frame.setWidth(abilities.length * (slotSize + gap) - gap)
      while slots.size < abilities.size do slots += Slot(slots.size)
      for (slot, i) <- slots.zipWithIndex do slot.update(player, abilities.lift(i), p.now)
    }
    if me.isEmpty then tooltip.hide()
  }

  /** Short enough for the corner of an icon: Digit1 is 1, KeyQ is Q, Shift KeyQ is S-Q, Button 2
    * (of the mouse) is M2.
    */
  private def shortLabel(label: String): String =
    label.replace("Shift ", "S-").replace("Digit", "").replace("Key", "").replace("Button ", "M")
}

object ActionBar {
  private val slotSize = 52.0
  private val gap      = 6.0
  private val iconSize = 40.0

  /** From the bottom of the HUD to the top of the bar. */
  val height: Double = slotSize + 8
}

/** Name and key, cast time, cooldown and cost, and what an ability does, above its slot (over
  * everything, and inside the HUD).
  */
private final class Tooltip(parent: Frame, i18n: I18n) {
  private val width   = 300.0
  private val padding = 10.0

  private val frame = Frame(parent)
  frame.setFrameStrata(Strata.Tooltip)
  frame.setWidth(width)
  frame.hide()

  private def line(size: Int, colour: Paint, justify: JustifyH = JustifyH.Left): FontString = {
    val fontString = HudWidgets.text(frame, size, colour, justify)
    fontString.setWordWrap(true)
    fontString
  }
  private val name = line(titleSize, textColour)
  name.setPoint(Point.TopLeft, frame, Point.TopLeft, padding, padding)
  name.setPoint(Point.TopRight, frame, Point.TopRight, -padding, padding)
  private val key = line(normalSize, mutedColour, JustifyH.Right)
  key.setPoint(Point.TopLeft, name, Point.TopLeft)
  key.setPoint(Point.BottomRight, name, Point.BottomRight)
  private val details = line(smallSize, mutedColour)
  details.setPoint(Point.TopLeft, name, Point.BottomLeft, 0, 6)
  details.setPoint(Point.TopRight, name, Point.BottomRight, 0, 6)
  private val description = line(normalSize, textColour)
  description.setPoint(Point.TopLeft, details, Point.BottomLeft, 0, 6)
  description.setPoint(Point.TopRight, details, Point.BottomRight, 0, 6)

  def show(doc: AbilityDoc, keyLabel: Option[String], slot: Frame): Unit = {
    name.setText(doc.name)
    key.setText(keyLabel.getOrElse(""))
    details.setText(
      (Seq(i18n.castingTime(doc.castingTimeRepr), i18n.cooldown(doc.cooldownRepr)) ++
        doc.maybeCostRepr.map(i18n.cost)).mkString("  ·  ")
    )
    description.setText(doc.description)
    frame.setBackdrop(
      Backdrop(
        Paint.rgb(30, 34, 46).withAlpha(0.95),
        Some(softened(Ability.abilityColour(doc.abilityId))),
        8
      )
    )
    // the texts' heights only depend on the width
    frame.setHeight(
      padding + name.stringHeight + 6 + details.stringHeight + 6 + description.stringHeight + padding
    )
    // above the slot, but inside the HUD (which is not at the window's left in the tutorial)
    val slotCentre = slot.left + slot.width / 2 - parent.left
    val centre     = math.max(4 + width / 2, math.min(parent.width - 4 - width / 2, slotCentre))
    frame.setPoint(Point.Bottom, slot, Point.Top, centre - slotCentre, -8)
    frame.show()
  }

  def hide(): Unit = frame.hide()
}
