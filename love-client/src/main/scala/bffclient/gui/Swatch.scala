package bffclient.gui

import bffclient.ui.{Colour, Theme}

/** A colour square, outlined when selected (or under the mouse). Clicked (`OnClick`) when enabled.
  */
class Swatch(parentFrame: Frame) extends Frame(parentFrame) {
  enableMouse(true)

  private var selected = false
  private var enabled  = true

  private val fill  = createTexture(Layer.Background)
  private val outer = createTexture(Colour(0, 0, 0, 0), Layer.Border)
  private val inner = createTexture(Colour(0, 0, 0, 0), Layer.Border)

  fill.setAllPoints(this)
  outer.setAllPoints(this)
  inner.setPoint(Point.TopLeft, this, Point.TopLeft, 1, 1)
  inner.setPoint(Point.BottomRight, this, Point.BottomRight, -1, -1)

  def setColour(colour: Colour): Unit      = fill.setColour(colour)
  def setSelected(selected: Boolean): Unit = this.selected = selected

  /** A disabled swatch only shows a colour. */
  def setEnabled(enabled: Boolean): Unit = {
    this.enabled = enabled
    enableMouse(enabled)
  }

  override protected[gui] def click(button: Int): Unit = if enabled then super.click(button)

  override protected[gui] def refresh(gui: Gui): Unit = {
    outer.setOutline(
      if selected then Some(Theme.text)
      else Option.when(enabled && gui.isHovered(this))(Theme.text.withAlpha(0.6))
    )
    inner.setOutline(Option.when(selected)(Theme.text))
  }
}

object Swatch {
  def apply(parent: Frame): Swatch = new Swatch(parent)

  def apply(parent: Frame, colour: Colour): Swatch = {
    val swatch = new Swatch(parent)
    swatch.setColour(colour)
    swatch
  }
}
