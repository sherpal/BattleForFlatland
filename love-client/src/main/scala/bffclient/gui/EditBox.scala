package bffclient.gui

import bffclient.love.Timer
import bffclient.ui.Theme

/** A single line text field. A click gives it the keyboard focus (see [[Gui.focus]]); then it gets
  * the text typed and the keys pressed: Enter, Tab and Escape fire `OnEnterPressed`, `OnTabPressed`
  * and `OnEscapePressed` (by default, Escape clears the focus).
  *
  * The text scrolls to keep its end visible while typing.
  */
class EditBox(parentFrame: Frame) extends Frame(parentFrame) {
  enableMouse(true)
  setClipsChildren(true)

  private val inset      = 8.0
  private var _text      = ""
  private var enabled    = true
  private var maxLetters = 32
  private var scrolled   = 0.0

  val fontString: FontString  = createFontString()
  val placeholder: FontString = createFontString(colour = Theme.muted)
  private val cursor          = createTexture(Theme.text, Layer.Overlay)

  placeText()
  placeholder.setPoint(Point.Left, this, Point.Left, inset, 0)
  cursor.setWidth(2)
  cursor.setPoint(Point.TopLeft, fontString, Point.TopRight, 1, 7)
  cursor.setPoint(Point.BottomLeft, fontString, Point.BottomRight, 1, -7)

  def text: String = _text

  /** Fires `OnTextChanged` when it changes. */
  def setText(text: String): Unit = {
    val kept = text.take(maxLetters)
    if kept != _text then {
      _text = kept
      fontString.setText(kept)
      fire(Script.OnTextChanged)(_(kept))
    }
  }

  /** In bytes (LÖVE's strings are UTF-8). */
  def setMaxLetters(max: Int): Unit = maxLetters = max

  /** Shown, in a muted colour, while it is empty and without focus. */
  def setPlaceholder(text: String): Unit = placeholder.setText(text)

  def isEnabled: Boolean = enabled
  def setEnabled(enabled: Boolean): Unit = {
    this.enabled = enabled
    if !enabled then clearFocus()
  }

  def hasFocus: Boolean  = attachedGui.exists(_.focus.exists(_ eq this))
  def setFocus(): Unit   = if enabled then attachedGui.foreach(_.setFocus(this))
  def clearFocus(): Unit = if hasFocus then gui.clearFocus()

  private def placeText(): Unit = {
    fontString.setPoint(Point.TopLeft, this, Point.TopLeft, inset - scrolled, 0)
    fontString.setPoint(Point.BottomLeft, this, Point.BottomLeft, inset - scrolled, 0)
  }

  private[gui] def insert(typed: String): Unit = if enabled then setText(_text + typed)

  override protected[gui] def mouseDown(button: Int): Unit = {
    if button == 1 then setFocus()
    super.mouseDown(button)
  }

  override protected[gui] def keyDown(scancode: String): Unit = {
    scancode match {
      case "backspace" => setText(EditBox.dropLastCharacter(_text))
      case "return" | "kpenter" => fire(Script.OnEnterPressed)(_())
      case "tab" => fire(Script.OnTabPressed)(_())
      case "escape" =>
        if hasScript(Script.OnEscapePressed) then fire(Script.OnEscapePressed)(_())
        else clearFocus()
      case _ => ()
    }
    super.keyDown(scancode)
  }

  override protected[gui] def refresh(gui: Gui): Unit = {
    val focused = gui.focus.exists(_ eq this)
    // the end of the text (and the cursor) stays visible while typing
    val overflow = math.max(0.0, fontString.stringWidth + 3 - (width - 2 * inset))
    val scroll   = if focused then overflow else 0.0
    if scroll != scrolled then { scrolled = scroll; placeText() }
    cursor.setShown(focused && (Timer.getTime() * 2).toInt % 2 == 0)
    placeholder.setShown(_text.isEmpty && !focused)
    fontString.setColour(if enabled then Theme.text else Theme.disabledText)
    setBackdrop(
      Backdrop(Theme.field, Some(if focused then Theme.primary.lighter(0.3) else Theme.border))
    )
  }
}

object EditBox {

  /** Removes the last (UTF-8) character: LÖVE's strings are bytes. */
  def dropLastCharacter(text: String): String = {
    var end = text.length - 1
    while end > 0 && (text.charAt(end) & 0xc0) == 0x80 do end -= 1
    text.substring(0, math.max(end, 0))
  }

  def apply(parent: Frame): EditBox = new EditBox(parent)

  def apply(parent: Frame, text: String, placeholder: String, maxLetters: Int): EditBox = {
    val box = new EditBox(parent)
    box.setMaxLetters(maxLetters)
    box.setText(text)
    box.setPlaceholder(placeholder)
    box
  }
}
