package bffclient.gui

import bffclient.love.Graphics
import bffclient.ui.{Colour, Rect}

/** Drawing with love.graphics, with an opacity (the frames') and rounded corners. */
private[gui] object Draw {

  def colour(colour: Colour, alpha: Double): Unit =
    Graphics.setColor(colour.r, colour.g, colour.b, colour.a * alpha)

  /** The corners' radius, at most half the width and height. */
  private def corner(rect: Rect, radius: Double): Double =
    math.max(0.0, math.min(radius, math.min(rect.width, rect.height) / 2))

  def fill(rect: Rect, c: Colour, alpha: Double, radius: Double = 0): Unit =
    if c.a > 0 && rect.width > 0 && rect.height > 0 then {
      colour(c, alpha)
      val r = corner(rect, radius)
      if r > 0 then Graphics.rectangle("fill", rect.x, rect.y, rect.width, rect.height, r, r)
      else Graphics.rectangle("fill", rect.x, rect.y, rect.width, rect.height)
    }

  /** Inside the rectangle. */
  def outline(rect: Rect, c: Colour, alpha: Double, radius: Double = 0, width: Double = 1): Unit = {
    colour(c, alpha)
    Graphics.setLineWidth(width)
    val inner =
      Rect(rect.x + width / 2, rect.y + width / 2, rect.width - width, rect.height - width)
    val r = corner(inner, radius)
    if r > 0 then Graphics.rectangle("line", inner.x, inner.y, inner.width, inner.height, r, r)
    else Graphics.rectangle("line", inner.x, inner.y, inner.width, inner.height)
    Graphics.setLineWidth(1)
  }
}
