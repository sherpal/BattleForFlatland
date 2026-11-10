package bffclient.hud

import scala.collection.mutable

import bffclient.gui.*
import bffclient.hud.HudStyle.*
import bffclient.render.{Painter, Sprite}
import bffclient.ui.Colour as Paint
import gamelogic.buffs.Buff
import gamelogic.entities.classes.PlayerClass

/** The frames of the HUD, in the [[HudStyle]]: translucent panels with rounded corners, bars with a
  * sheen, texts with a shadow.
  */
object HudWidgets {
  val panelBackdrop: Backdrop = panelBackdrop(6)

  def panelBackdrop(radius: Double): Backdrop =
    Backdrop(panelColour, Some(borderColour), radius, Some(Paint(1, 1, 1, 0.1)))

  def panel(parent: Frame, radius: Double = 6): Frame = {
    val frame = Frame(parent)
    frame.setBackdrop(panelBackdrop(radius))
    frame
  }

  /** A text with a shadow, over the other regions of the frame. */
  def text(
      parent: Frame,
      size: Int = normalSize,
      colour: Paint = textColour,
      justify: JustifyH = JustifyH.Left
  ): FontString = {
    val text = parent.createFontString(size = size, colour = colour, layer = Layer.Overlay)
    text.setShadow(Some(Paint(0, 0, 0, 0.7)))
    text.setJustifyH(justify)
    text
  }

  /** A text over `region`, `padding` from its sides, vertically centred (small in thin regions,
    * like the bars).
    */
  def textOver(
      parent: Frame,
      region: Region,
      justify: JustifyH = JustifyH.Left,
      colour: Paint = textColour,
      size: Int = -1,
      padding: Double = 4
  ): FontString = {
    val fontString = text(
      parent,
      if size > 0 then size else if region.height < 15 then smallSize else normalSize,
      colour,
      justify
    )
    fontString.setPoint(Point.TopLeft, region, Point.TopLeft, padding, 0)
    fontString.setPoint(Point.BottomRight, region, Point.BottomRight, -padding, 0)
    fontString
  }

  /** Puts `region` at (x, y) of `frame`'s top left corner, with this size. */
  def place(
      region: Region,
      frame: Region,
      x: Double,
      y: Double,
      width: Double,
      height: Double
  ): Unit = {
    region.setPoint(Point.TopLeft, frame, Point.TopLeft, x, y)
    region.setSize(width, height)
  }

  /** The sprite's first frame, on the texture. */
  def setSprite(texture: Texture, sprite: Sprite): Unit =
    texture.setImage(sprite.image, sprite.quad(), sprite.frameWidth, sprite.frameHeight)
}

/** A bar filled in a colour on a dark track (rounded, with a sheen on its upper part), with a text
  * on its left and one on its right. To be given a size (which chooses the texts' size).
  */
final class HudBar(parentFrame: Frame, width: Double, height: Double)
    extends StatusBar(parentFrame) {
  setSize(width, height)
  private val radius = math.min(3.0, height / 2)
  background.setColour(trackColour)
  background.setCornerRadius(radius)
  bar.setCornerRadius(radius)
  private val sheen = createTexture(Paint(1, 1, 1, 0.2), Layer.Border)
  sheen.setPoint(Point.TopLeft, bar, Point.TopLeft, 1, 1)
  sheen.setPoint(Point.BottomRight, bar, Point(1, 0.4), -1, 0)

  val label: FontString = HudWidgets.textOver(this, this)
  val value: FontString = HudWidgets.textOver(this, this, JustifyH.Right)

  def setColour(colour: Paint): Unit = bar.setColour(colour)
}

/** The buffs' icons in a row, the ones that end darkened from the top by the part of their duration
  * that remains (like the frontend), with a border in the colour of the player who put them (see
  * [[Painter.buffSourceColour]]). Hovering an icon shows the name of its buff in a [[BuffTooltip]],
  * in `tooltipParent` (which should not fade).
  */
final class BuffRow(
    parent: Frame,
    tooltipParent: Frame,
    iconSize: Double,
    spacing: Double,
    maxIcons: Int
) {
  val frame: Frame = Frame(parent)
  frame.setSize(maxIcons * spacing, iconSize)

  private val tooltip = BuffTooltip(tooltipParent)

  private final class Icon(index: Int) {
    // Over the icon, for its tooltip only: the clicks go to the frame under it
    val button = Frame(frame)
    HudWidgets.place(button, frame, index * spacing, 0, iconSize, iconSize)
    button.enableMouse(true)
    button.registerForClicks()
    button.setScript(Script.OnLeave)(() => tooltip.hide())
    val icon = frame.createTexture(Layer.Artwork)
    icon.setAllPoints(button)
    val shade = frame.createTexture(Paint(0, 0, 0, 0.5), Layer.Overlay)
    shade.setPoint(Point.TopLeft, icon, Point.TopLeft)
    // A thin dark border, like the ability icons', or in the colour of the player who put the buff
    val border = frame.createTexture(Paint(0, 0, 0, 0), Layer.Overlay)
    border.setPoint(Point.TopLeft, icon, Point.TopLeft, -1, -1)
    border.setPoint(Point.BottomRight, icon, Point.BottomRight, 1, 1)
    border.setCornerRadius(2)
    private var remaining = -1.0

    def update(buff: Option[(Buff, Sprite)], p: Painter): Unit = {
      for t <- Seq(icon, shade, border) do t.setShown(buff.isDefined)
      button.setShown(buff.isDefined)
      if frame.gui.isHovered(button) then
        buff.fold(tooltip.hide())((b, _) => tooltip.show(b, p.buffSourceColour(b), button))
      for (b, sprite) <- buff do {
        HudWidgets.setSprite(icon, sprite)
        border.setOutline(Some(p.buffSourceColour(b).getOrElse(Paint(0, 0, 0, 0.85))))
        val left = if b.isFinite then b.remainingPercentage(p.gameState.time) else 0.0
        shade.setShown(left > 0)
        if left != remaining then {
          remaining = left
          shade.setPoint(Point.BottomRight, icon, Point(1, left))
        }
      }
    }
  }

  private val icons = mutable.ArrayBuffer.empty[Icon]

  def update(buffs: Seq[(Buff, Sprite)], p: Painter): Unit = {
    val shown = buffs.take(maxIcons)
    while icons.size < shown.size do icons += Icon(icons.size)
    for (icon, i) <- icons.zipWithIndex do icon.update(shown.lift(i), p)
  }
}

/** The name of a buff, below its icon (over everything), bordered with the colour of the player who
  * put it (like the icon).
  */
final class BuffTooltip(parent: Frame) {
  private val padding = 6.0
  private val height  = 22.0

  private val frame = Frame(parent)
  frame.setFrameStrata(Strata.Tooltip)
  frame.hide()
  private val name = HudWidgets.text(frame, justify = JustifyH.Center)
  name.setAllPoints(frame)
  private var anchor = Option.empty[Frame]

  def show(buff: Buff, source: Option[Paint], icon: Frame): Unit = {
    name.setText(buff.name)
    frame.setSize(name.stringWidth + 2 * padding, height)
    if !anchor.exists(_ eq icon) then {
      anchor = Some(icon)
      frame.setPoint(Point.TopLeft, icon, Point.BottomLeft, 0, 4)
    }
    frame.setBackdrop(
      Backdrop(Paint.rgb(30, 34, 46).withAlpha(0.95), Some(source.getOrElse(borderColour)), 6)
    )
    frame.show()
  }

  def hide(): Unit = frame.hide()
}

/** The class of the player in a round badge, in their colour, centred where it is anchored. */
final class ClassBadge(parent: Frame) {
  val frame: Frame = Frame(parent)
  frame.setSize(19, 19)
  private val disc = frame.createTexture(Paint(0, 0, 0, 0.4), Layer.Artwork)
  HudWidgets.place(disc, frame, 0.5, 0.5, 18, 18)
  disc.setCornerRadius(9)
  private val shape = frame.createTexture(Layer.Overlay)
  private val ring  = frame.createTexture(Paint(0, 0, 0, 0), Layer.Overlay)
  ring.setAllPoints(frame)
  ring.setCornerRadius(9.5)
  ring.setOutline(Some(Paint(0, 0, 0, 0.85)))
  private var shownClass = ""

  def update(player: PlayerClass): Unit = {
    val (r, g, b) = player.rgb
    shape.setColour(Paint(r / 255.0, g / 255.0, b / 255.0))
    if player.cls.value != shownClass then {
      shownClass = player.cls.value
      bffclient.render.Sprites.playerClasses.get(shownClass) match {
        case Some(sprite) =>
          HudWidgets.setSprite(shape, sprite)
          shape.setCornerRadius(0)
          shape.setSize(14, 14 * sprite.frameHeight / sprite.frameWidth)
        case None =>
          shape.clearImage()
          shape.setCornerRadius(6)
          shape.setSize(12, 12)
      }
      shape.setPoint(Point.Center, frame, Point.Center)
    }
  }
}
