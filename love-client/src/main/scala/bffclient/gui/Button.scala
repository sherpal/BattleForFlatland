package bffclient.gui

import bffclient.ui.{ButtonStyle, Colour, Theme}

/** A button, like WoW's: its normal, pushed, highlight (mouse over) and disabled textures are shown
  * depending on its state, under its text. It is only clicked when enabled.
  */
class Button(parentFrame: Frame) extends Frame(parentFrame) {
  enableMouse(true)

  private var enabled            = true
  private var textColour         = Theme.text
  private var disabledTextColour = Theme.disabledText

  val normalTexture: Texture    = createTexture(Layer.Background)
  val pushedTexture: Texture    = createTexture(Layer.Background)
  val disabledTexture: Texture  = createTexture(Theme.disabled, Layer.Background)
  val highlightTexture: Texture = createTexture(Layer.Border)
  val fontString: FontString    = createFontString(layer = Layer.Overlay)

  for texture <- Seq(normalTexture, pushedTexture, disabledTexture, highlightTexture) do
    texture.setAllPoints(this)
  fontString.setAllPoints(this)
  fontString.setJustifyH(JustifyH.Center)
  setStyle(ButtonStyle.Normal)

  /** The colours of the theme's buttons. */
  def setStyle(style: ButtonStyle): Unit = {
    normalTexture.setColour(style.colour)
    highlightTexture.setColour(style.colour.lighter(0.15))
    pushedTexture.setColour(style.colour.lighter(0.3))
  }

  def text: String                = fontString.text
  def setText(text: String): Unit = fontString.setText(text)

  def setTextColours(normal: Colour, disabled: Colour): Unit = {
    textColour = normal
    disabledTextColour = disabled
  }

  def isEnabled: Boolean                 = enabled
  def setEnabled(enabled: Boolean): Unit = this.enabled = enabled
  def enable(): Unit                     = setEnabled(true)
  def disable(): Unit                    = setEnabled(false)

  override protected[gui] def click(button: Int): Unit = if enabled then super.click(button)

  override protected[gui] def refresh(gui: Gui): Unit = {
    val hovered = enabled && gui.isHovered(this)
    val pushed  = hovered && gui.isPressed(this)
    normalTexture.setShown(enabled && !pushed)
    pushedTexture.setShown(pushed)
    highlightTexture.setShown(hovered && !pushed)
    disabledTexture.setShown(!enabled)
    fontString.setColour(if enabled then textColour else disabledTextColour)
  }
}

object Button {
  def apply(parent: Frame): Button = new Button(parent)

  def apply(
      parent: Frame,
      text: String,
      style: ButtonStyle = ButtonStyle.Normal
  ): Button = {
    val button = new Button(parent)
    button.setText(text)
    button.setStyle(style)
    button
  }
}
