package bffclient.gui

import bffclient.love.Mouse
import bffclient.ui.Theme

/** A value from `min` to `max` by `step`: a track filled up to it, with a knob, and the value on
  * the right. The player drags the knob (or presses where it should go), or turns the mouse wheel
  * over it (a step per turn). Fires its `OnValueChanged` when the player changes the value (once
  * per step while dragging): `slider.setScript(slider.OnValueChanged)(value => ...)`.
  */
class Slider(
    parentFrame: Frame,
    min: Double,
    max: Double,
    step: Double,
    show: Double => String = (value: Double) => value.toString
) extends Frame(parentFrame) {
  require(min < max && step > 0, s"Slider from $min to $max by $step")

  val OnValueChanged: Script[Double => Unit] = Script("OnValueChanged")

  enableMouse(true)
  registerForClicks(1)
  enableMouseWheel(true)

  private val steps   = math.round((max - min) / step).toInt
  private var index   = 0
  private var enabled = true

  private val track = createTexture(Theme.field, Layer.Background)
  private val fill  = createTexture(Theme.primary, Layer.Artwork)
  private val knob  = createTexture(Layer.Overlay)
  private val text  = createFontString()

  track.setPoint(Point.Left, this, Point.Left, Slider.knobWidth / 2, 0)
  track.setPoint(Point.Right, this, Point.Right, -Slider.textWidth - Slider.knobWidth / 2, 0)
  track.setHeight(6)
  track.setCornerRadius(3)
  fill.setPoint(Point.TopLeft, track, Point.TopLeft)
  fill.setCornerRadius(3)
  knob.setSize(Slider.knobWidth, 20)
  knob.setCornerRadius(4)
  text.setPoint(Point.TopRight, this, Point.TopRight)
  text.setPoint(Point.BottomRight, this, Point.BottomRight)
  text.setWidth(Slider.textWidth)
  text.setJustifyH(JustifyH.Right)
  placeKnob()

  def value: Double = min + index * step

  /** Shows the value (the closest step to it, without firing `OnValueChanged`). */
  def setValue(value: Double): Unit = {
    index = indexOf(value)
    placeKnob()
  }

  def isEnabled: Boolean                 = enabled
  def setEnabled(enabled: Boolean): Unit = this.enabled = enabled

  private def indexOf(value: Double): Int =
    math.max(0, math.min(steps, math.round((value - min) / step).toInt))

  private def placeKnob(): Unit = {
    val fraction = index.toDouble / steps
    fill.setPoint(Point.BottomRight, track, Point(fraction, 1))
    fill.setShown(index > 0)
    knob.setPoint(Point.Center, track, Point(fraction, 0.5))
    text.setText(show(value))
  }

  private def choose(newIndex: Int): Unit = if newIndex != index then {
    index = newIndex
    placeKnob()
    fire(OnValueChanged)(_(value))
  }

  /** To the step closest to the mouse. */
  private def follow(): Unit = {
    val mouseX   = Mouse.getPosition().x / drawScale
    val fraction = (mouseX - track.left) / track.width
    choose(math.max(0, math.min(steps, math.round(fraction * steps).toInt)))
  }

  override protected[gui] def mouseDown(button: Int): Unit = {
    if enabled then follow()
    super.mouseDown(button)
  }

  override protected[gui] def mouseWheel(delta: Double): Unit = {
    if enabled then choose(math.max(0, math.min(steps, index + math.signum(delta).toInt)))
    super.mouseWheel(delta)
  }

  override protected[gui] def refresh(gui: Gui): Unit = {
    if enabled && gui.isPressed(this) then follow()
    val active = enabled && (gui.isHovered(this) || gui.isPressed(this))
    fill.setColour(if enabled then Theme.primary else Theme.disabled)
    knob.setColour(
      if !enabled then Theme.disabledText
      else if active then Theme.primary.lighter(0.5)
      else Theme.text
    )
    text.setColour(if enabled then Theme.text else Theme.disabledText)
  }
}

object Slider {
  private val knobWidth = 12.0

  /** Room for the value on the right. */
  private val textWidth = 44.0
}
