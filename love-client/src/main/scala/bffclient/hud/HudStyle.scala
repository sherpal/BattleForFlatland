package bffclient.hud

import bffclient.love.Graphics
import bffclient.ui.{Colour as Paint, Rect}
import utils.misc.RGBColour

/** The look of the HUD: dark translucent panels with rounded corners and a thin light border,
  * rounded bars on a dark track with a sheen, and white text with a shadow (see [[HudWidgets]]).
  * The bars are also drawn over the entities of the world (see `Painter`).
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

  def ratio(value: Double, max: Double): Double =
    if max <= 0 then 0 else math.max(0.0, math.min(1.0, value / max))
}
