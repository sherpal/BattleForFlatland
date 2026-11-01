package bffclient.gui

import bffclient.ui.Theme

/** A check box followed by its label. A click (when enabled) checks or unchecks it, then fires
  * `OnClick`, whose handlers read [[isChecked]].
  */
class CheckButton(parentFrame: Frame) extends Frame(parentFrame) {
  enableMouse(true)

  private var checked = false
  private var enabled = true

  private val box            = createTexture(Theme.field, Layer.Background)
  private val check          = createTexture(Layer.Artwork)
  val fontString: FontString = createFontString()

  box.setSize(22, 22)
  box.setPoint(Point.Left, this, Point.Left)
  check.setPoint(Point.TopLeft, box, Point.TopLeft, 5, 5)
  check.setPoint(Point.BottomRight, box, Point.BottomRight, -5, -5)
  fontString.setPoint(Point.Left, this, Point.Left, 30, 0)

  override protected def intrinsicWidth: Double  = 30 + fontString.stringWidth
  override protected def intrinsicHeight: Double = 22

  def text: String                = fontString.text
  def setText(text: String): Unit = fontString.setText(text)

  def isChecked: Boolean                 = checked
  def setChecked(checked: Boolean): Unit = this.checked = checked

  def isEnabled: Boolean                 = enabled
  def setEnabled(enabled: Boolean): Unit = this.enabled = enabled

  override protected[gui] def click(button: Int): Unit = if enabled then {
    checked = !checked
    super.click(button)
  }

  override protected[gui] def refresh(gui: Gui): Unit = {
    box.setOutline(
      Some(if enabled && gui.isHovered(this) then Theme.primary.lighter(0.3) else Theme.border)
    )
    check.setShown(checked)
    check.setColour(if enabled then Theme.positive.lighter(0.2) else Theme.disabledText)
    fontString.setColour(if enabled then Theme.text else Theme.disabledText)
  }
}

object CheckButton {
  def apply(parent: Frame): CheckButton = new CheckButton(parent)

  def apply(parent: Frame, text: String): CheckButton = {
    val checkButton = new CheckButton(parent)
    checkButton.setText(text)
    checkButton
  }
}
