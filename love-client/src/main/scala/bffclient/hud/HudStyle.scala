package bffclient.hud

import scala.collection.mutable

import bffclient.love.{Font, Graphics}
import bffclient.lua.ScalaLove
import bffclient.ui.{Colour as Paint, Rect}
import utils.misc.RGBColour

/** The look of the HUD: dark translucent panels with rounded corners and a thin light border,
  * rounded bars on a dark track with a sheen, and white text with a shadow.
  */
object HudStyle {
  val panelColour  = Paint.rgb(52, 58, 76).withAlpha(0.8)
  val borderColour = Paint(1, 1, 1, 0.16)
  val trackColour  = Paint(0, 0, 0, 0.45)
  val textColour   = Paint.rgb(242, 243, 247)
  val mutedColour  = Paint.rgb(160, 168, 188)
  val targetColour = Paint.rgb(255, 90, 70)
  val startColour  = Paint.rgb(46, 160, 90)

  /** Text sizes, in pixels at scale 1. */
  val normalSize = 12
  val smallSize  = 10
  val titleSize  = 14
  val largeSize  = 20
  val hugeSize   = 72

  /** The scale of the HUD while it is drawn (see [[scaled]]). */
  private var currentScale = 1.0

  private val fonts = mutable.Map.empty[Int, Font]

  private def fontOfSize(pixels: Int): Font =
    fonts.getOrElseUpdate(pixels, Graphics.newFont(pixels))

  private def scaledFont(size: Int): Font =
    fontOfSize(math.max(1, math.round(size * currentScale).toInt))

  /** Draws the HUD at this scale (the `hudScale` setting). Its texts are drawn with fonts of the
    * scaled size, rather than scaled, so that they stay sharp.
    */
  def scaled(scale: Double)(draw: => Unit): Unit = {
    currentScale = scale
    Graphics.push()
    Graphics.scale(scale)
    try draw
    finally {
      Graphics.pop()
      currentScale = 1.0
    }
  }

  /** The height of a line of text of `size` pixels, in the HUD's coordinates. */
  def lineHeight(size: Int = normalSize): Double = scaledFont(size).getHeight() / currentScale

  /** The lines of `value` wrapped at `width` (in the HUD's coordinates), in a font of `size`. */
  def wrap(value: String, width: Double, size: Int = normalSize): Seq[String] =
    ScalaLove.fromLuaSequence(scaledFont(size).getWrap(value, width * currentScale).lines).toSeq

  /** Text at (x, y), in the HUD's coordinates, in the current colour, sharp at any scale. */
  def print(
      value: String,
      x: Double,
      y: Double,
      limit: Double,
      align: String = "left",
      size: Int = normalSize
  ): Unit = {
    val previousFont = Graphics.getFont()
    Graphics.setFont(scaledFont(size))
    Graphics.push()
    Graphics.scale(1 / currentScale)
    Graphics.printf(
      value,
      math.round(x * currentScale).toDouble,
      math.round(y * currentScale).toDouble,
      limit * currentScale,
      align
    )
    Graphics.pop()
    Graphics.setFont(previousFont)
  }

  private def set(colour: Paint, alpha: Double): Unit =
    Graphics.setColor(colour.r, colour.g, colour.b, colour.a * alpha)

  def paint(colour: RGBColour): Paint = Paint.rgb(colour.red, colour.green, colour.blue)

  /** Lighter, for the colours of the game (e.g. pure blue) on the dark panels. */
  def softened(colour: RGBColour): Paint = paint(colour).lighter(0.25)

  /** Green, then orange, then red (like the frontend's `StatusBar.lifeStatusColor`). */
  def lifeColour(ratio: Double): Paint =
    if ratio > 0.5 then Paint.rgb(70, 200, 95)
    else if ratio > 0.2 then Paint.rgb(240, 165, 45)
    else Paint.rgb(225, 65, 60)

  def roundedFill(rect: Rect, radius: Double, colour: Paint, alpha: Double = 1.0): Unit = {
    set(colour, alpha)
    Graphics.rectangle("fill", rect.x, rect.y, rect.width, rect.height, radius, radius)
  }

  def roundedLine(
      rect: Rect,
      radius: Double,
      colour: Paint,
      lineWidth: Double = 1,
      alpha: Double = 1.0
  ): Unit = {
    set(colour, alpha)
    Graphics.setLineWidth(lineWidth)
    Graphics.rectangle(
      "line",
      rect.x + 0.5,
      rect.y + 0.5,
      rect.width - 1,
      rect.height - 1,
      radius,
      radius
    )
    Graphics.setLineWidth(1)
  }

  /** A panel, with a lighter top edge (lit from above) and a thin border. */
  def panel(rect: Rect, alpha: Double = 1.0, radius: Double = 6): Unit = {
    roundedFill(rect, radius, panelColour, alpha)
    set(Paint(1, 1, 1, 0.1), alpha)
    Graphics.line(rect.x + radius, rect.y + 1.5, rect.right - radius, rect.y + 1.5)
    roundedLine(rect, radius, borderColour, 1, alpha)
  }

  /** The glow around the panel of the target. */
  def targetGlow(rect: Rect, radius: Double = 6): Unit = {
    roundedLine(
      Rect(rect.x - 3, rect.y - 3, rect.width + 6, rect.height + 6),
      radius + 3,
      targetColour.withAlpha(0.3),
      4
    )
    roundedLine(rect, radius, targetColour, 2)
  }

  /** A bar: filled in `colour` up to `ratio` (between 0 and 1) on a dark track. */
  def bar(rect: Rect, ratio: Double, colour: Paint, alpha: Double = 1.0): Unit = {
    val radius = math.min(3.0, rect.height / 2)
    roundedFill(rect, radius, trackColour, alpha)
    val width = rect.width * math.max(0.0, math.min(1.0, ratio))
    if width >= 1 then {
      roundedFill(
        Rect(rect.x, rect.y, width, rect.height),
        math.min(radius, width / 2),
        colour,
        alpha
      )
      // A sheen on the upper part
      if width > 2 then {
        set(Paint(1, 1, 1, 0.2), alpha)
        Graphics.rectangle("fill", rect.x + 1, rect.y + 1, width - 2, rect.height * 0.4)
      }
    }
  }

  /** Text with a shadow, vertically centred in `rect`, of the normal size (small in thin
    * rectangles), unless another one is given.
    */
  def text(
      value: String,
      rect: Rect,
      align: String = "left",
      colour: Paint = textColour,
      alpha: Double = 1.0,
      padding: Double = 4,
      textSize: Option[Int] = None
  ): Unit = {
    val size  = textSize.getOrElse(if rect.height < 15 then smallSize else normalSize)
    val y     = rect.y + (rect.height - lineHeight(size)) / 2
    val x     = rect.x + padding
    val limit = rect.width - 2 * padding
    set(Paint(0, 0, 0, 0.7), alpha)
    print(value, x + 1, y + 1, limit, align, size)
    set(colour, alpha)
    print(value, x, y, limit, align, size)
  }

  def ratio(value: Double, max: Double): Double =
    if max <= 0 then 0 else math.max(0.0, math.min(1.0, value / max))
}
