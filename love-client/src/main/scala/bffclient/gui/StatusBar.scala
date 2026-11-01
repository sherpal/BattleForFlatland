package bffclient.gui

import bffclient.ui.Theme

/** A bar filled up to a fraction of its width (WoW's StatusBar, e.g. a progress bar). */
class StatusBar(parentFrame: Frame) extends Frame(parentFrame) {
  val background: Texture = createTexture(Theme.field, Layer.Background)
  val bar: Texture        = createTexture(Theme.primary, Layer.Artwork)

  private var value = -1.0

  background.setAllPoints(this)
  bar.setPoint(Point.TopLeft, this, Point.TopLeft)
  setValue(0)

  def getValue: Double = value

  /** Between 0 (empty) and 1 (full). */
  def setValue(fraction: Double): Unit = {
    val clamped = math.max(0.0, math.min(1.0, fraction))
    if clamped != value then {
      value = clamped
      bar.setPoint(Point.BottomRight, this, Point(clamped, 1))
    }
  }
}
