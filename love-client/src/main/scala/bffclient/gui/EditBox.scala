package bffclient.gui

import bffclient.love.{Keyboard, LoveSystem, Mouse, Timer}
import bffclient.ui.Theme

/** A single line text field. A click gives it the keyboard focus (see [[Gui.focus]]); then it gets
  * the text typed and the keys pressed: Enter, Tab and Escape fire `OnEnterPressed`, `OnTabPressed`
  * and `OnEscapePressed` (by default, Escape clears the focus).
  *
  * Like the usual text fields, it has a caret and a selection:
  *   - Left and Right move the caret by a character (by a word with Ctrl or Alt), Home and End to
  *     the start and the end; with Shift, they extend the selection;
  *   - Backspace and Delete remove the selection, or a character (a word with Ctrl or Alt);
  *   - Ctrl (or Cmd) + A selects everything, + C copies, + X cuts and + V pastes (on one line);
  *   - a click places the caret (with Shift, extends the selection), dragging selects, a double
  *     click selects a word, and a triple click everything.
  *
  * The text scrolls to keep the caret visible. Its positions are bytes (LÖVE's strings are UTF-8),
  * always at the start of a character.
  */
class EditBox(parentFrame: Frame) extends Frame(parentFrame) {
  enableMouse(true)
  setClipsChildren(true)

  private val inset      = 8.0
  private var _text      = ""
  private var enabled    = true
  private var maxLetters = 32
  private var scrolled   = 0.0

  /** Where the caret is, and the other end of the selection (the selection is empty if equal). */
  private var caret     = 0
  private var anchor    = 0
  private var dragging  = false
  private var lastMove  = 0.0
  private var lastClick = -1.0
  private var clicks    = 0

  val fontString: FontString  = createFontString()
  val placeholder: FontString = createFontString(colour = Theme.muted)
  private val cursor          = createTexture(Theme.text, Layer.Overlay)
  private val selection       = createTexture(Theme.primary.withAlpha(0.45), Layer.Border)

  placeText()
  placeholder.setPoint(Point.Left, this, Point.Left, inset, 0)
  cursor.setWidth(2)
  selection.hide()

  def text: String = _text

  /** Fires `OnTextChanged` when it changes. The caret goes to the end. */
  def setText(text: String): Unit = {
    replaceText(EditBox.truncate(text, maxLetters))
    caret = _text.length
    anchor = caret
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

  /** The selected text (empty without selection). */
  def selectedText: String = _text.substring(selectionStart, selectionEnd)

  def selectAll(): Unit = { anchor = 0; moveCaret(_text.length, extend = true) }

  private def selectionStart: Int   = math.min(caret, anchor)
  private def selectionEnd: Int     = math.max(caret, anchor)
  private def hasSelection: Boolean = caret != anchor

  private def replaceText(text: String): Unit = if text != _text then {
    _text = text
    fontString.setText(text)
    fire(Script.OnTextChanged)(_(text))
  }

  private def moveCaret(to: Int, extend: Boolean): Unit = {
    caret = math.max(0, math.min(_text.length, to))
    if !extend then anchor = caret
    lastMove = Timer.getTime()
  }

  /** Replaces the selection with `typed` (as much of it as `maxLetters` allows). */
  private def replaceSelection(typed: String): Unit = {
    val start = selectionStart
    val end   = selectionEnd
    val room  = maxLetters - (_text.length - (end - start))
    val kept  = EditBox.truncate(typed, room)
    replaceText(_text.substring(0, start) + kept + _text.substring(end))
    moveCaret(start + kept.length, extend = false)
  }

  /** Removes the selection, or from the caret to `to` without selection. */
  private def delete(to: => Int): Unit = if hasSelection then replaceSelection("")
  else {
    val target = to
    anchor = target
    replaceSelection("")
  }

  private[gui] def insert(typed: String): Unit = if enabled then replaceSelection(typed)

  // --- Mouse ---

  /** The position in the text under the mouse (the closest start of a character). */
  private def positionUnderMouse: Int = {
    val x        = Mouse.getPosition().x / drawScale - fontString.left
    var position = 0
    var found    = false
    while !found && position < _text.length do {
      val next  = EditBox.nextCharacter(_text, position)
      val left  = fontString.textWidth(_text.substring(0, position))
      val right = fontString.textWidth(_text.substring(0, next))
      if x < (left + right) / 2 then found = true else position = next
    }
    position
  }

  override protected[gui] def mouseDown(button: Int): Unit = {
    if button == 1 && enabled then {
      val focused = hasFocus
      setFocus()
      val now = Timer.getTime()
      clicks = if now - lastClick < 0.4 then clicks + 1 else 1
      lastClick = now
      val position = positionUnderMouse
      clicks match {
        case 1 =>
          moveCaret(position, extend = focused && EditBox.shiftDown)
          dragging = true
        case 2 =>
          val (start, end) = EditBox.wordAround(_text, position)
          anchor = start
          moveCaret(end, extend = true)
        case _ => selectAll()
      }
    }
    super.mouseDown(button)
  }

  override protected[gui] def mouseUp(button: Int): Unit = {
    if button == 1 then dragging = false
    super.mouseUp(button)
  }

  // --- Keyboard ---

  override protected[gui] def keyDown(scancode: String): Unit = {
    val shift    = EditBox.shiftDown
    val word     = EditBox.wordModifierDown
    val shortcut = EditBox.shortcutDown
    def previous = if word then EditBox.previousWord(_text, caret)
    else EditBox.previousCharacter(_text, caret)
    def next = if word then EditBox.nextWord(_text, caret) else EditBox.nextCharacter(_text, caret)
    scancode match {
      case "left" =>
        if hasSelection && !shift then moveCaret(selectionStart, extend = false)
        else moveCaret(previous, shift)
      case "right" =>
        if hasSelection && !shift then moveCaret(selectionEnd, extend = false)
        else moveCaret(next, shift)
      case "home" => moveCaret(0, shift)
      case "end" => moveCaret(_text.length, shift)
      case "backspace" => if enabled then delete(previous)
      case "delete" => if enabled then delete(next)
      case "return" | "kpenter" => fire(Script.OnEnterPressed)(_())
      case "tab" => fire(Script.OnTabPressed)(_())
      case "escape" =>
        if hasScript(Script.OnEscapePressed) then fire(Script.OnEscapePressed)(_())
        else clearFocus()
      case _ if shortcut =>
        // the letter of the keyboard's layout, not the place of the key
        Keyboard.getKeyFromScancode(scancode) match {
          case "a" => selectAll()
          case "c" => if hasSelection then LoveSystem.setClipboardText(selectedText)
          case "x" =>
            if hasSelection then {
              LoveSystem.setClipboardText(selectedText)
              if enabled then replaceSelection("")
            }
          case "v" =>
            if enabled then replaceSelection(EditBox.oneLine(LoveSystem.getClipboardText()))
          case _ => ()
        }
      case _ => ()
    }
    super.keyDown(scancode)
  }

  // --- Drawing ---

  private def placeText(): Unit = {
    fontString.setPoint(Point.TopLeft, this, Point.TopLeft, inset - scrolled, 0)
    fontString.setPoint(Point.BottomLeft, this, Point.BottomLeft, inset - scrolled, 0)
  }

  override protected[gui] def refresh(gui: Gui): Unit = {
    val focused = gui.focus.exists(_ eq this)
    if !focused then { dragging = false; anchor = caret }
    if dragging then {
      val position = positionUnderMouse
      if position != caret then moveCaret(position, extend = true)
    }

    // the caret stays visible: the text scrolls when it would leave the box
    val caretX  = fontString.textWidth(_text.substring(0, caret))
    val visible = width - 2 * inset - 3
    val scroll =
      if !focused then 0.0
      else {
        val max = math.max(0.0, fontString.stringWidth - visible)
        math.max(0.0, math.min(max, math.max(caretX - visible, math.min(scrolled, caretX))))
      }
    if scroll != scrolled then { scrolled = scroll; placeText() }

    // solid while it moves, then blinking
    cursor.setShown(focused && (((Timer.getTime() - lastMove) * 2).toInt % 2 == 0))
    cursor.setPoint(Point.TopLeft, fontString, Point.TopLeft, caretX, 7)
    cursor.setPoint(Point.BottomLeft, fontString, Point.BottomLeft, caretX, -7)

    selection.setShown(focused && hasSelection)
    if focused && hasSelection then {
      val start = fontString.textWidth(_text.substring(0, selectionStart))
      selection.setPoint(Point.TopLeft, fontString, Point.TopLeft, start, 6)
      selection.setPoint(Point.BottomLeft, fontString, Point.BottomLeft, start, -6)
      selection.setWidth(fontString.textWidth(selectedText))
    }

    placeholder.setShown(_text.isEmpty && !focused)
    fontString.setColour(if enabled then Theme.text else Theme.disabledText)
    setBackdrop(
      Backdrop(Theme.field, Some(if focused then Theme.primary.lighter(0.3) else Theme.border))
    )
  }
}

object EditBox {

  private def isContinuation(text: String, i: Int): Boolean =
    i < text.length && (text.charAt(i) & 0xc0) == 0x80

  /** The start of the character before `position`. */
  def previousCharacter(text: String, position: Int): Int = {
    var i = math.max(0, position - 1)
    while i > 0 && isContinuation(text, i) do i -= 1
    i
  }

  /** The start of the character after the one at `position`. */
  def nextCharacter(text: String, position: Int): Int = {
    var i = math.min(text.length, position + 1)
    while isContinuation(text, i) do i += 1
    i
  }

  /** Letters, digits, `_`, and the bytes of non ASCII characters (accented letters, ...). */
  private def isWordByte(c: Char): Boolean = c.isLetterOrDigit || c == '_' || c >= 0x80

  /** The start of the word before `position` (skipping what separates them). */
  def previousWord(text: String, position: Int): Int = {
    var i = position
    while i > 0 && !isWordByte(text.charAt(i - 1)) do i -= 1
    while i > 0 && isWordByte(text.charAt(i - 1)) do i -= 1
    i
  }

  /** The end of the word after `position` (skipping what separates them). */
  def nextWord(text: String, position: Int): Int = {
    var i = position
    while i < text.length && !isWordByte(text.charAt(i)) do i += 1
    while i < text.length && isWordByte(text.charAt(i)) do i += 1
    i
  }

  /** The word at `position` (or the run of separators there), as (start, end). */
  def wordAround(text: String, position: Int): (Int, Int) =
    if text.isEmpty then (0, 0)
    else {
      val at    = math.min(position, text.length - 1)
      val inner = isWordByte(text.charAt(at))
      var start = at
      while start > 0 && isWordByte(text.charAt(start - 1)) == inner do start -= 1
      var end = at
      while end < text.length && isWordByte(text.charAt(end)) == inner do end += 1
      (start, end)
    }

  /** At most `max` bytes of `text`, without cutting a character. */
  def truncate(text: String, max: Int): String =
    if text.length <= max then text
    else {
      var end = math.max(0, max)
      while end > 0 && isContinuation(text, end) do end -= 1
      text.substring(0, end)
    }

  /** Pasted text, on one line. */
  def oneLine(text: String): String =
    text.replace("\r\n", " ").replace('\n', ' ').replace('\r', ' ').replace('\t', ' ')

  private def down(scancodes: String*): Boolean = scancodes.exists(Keyboard.isScancodeDown)

  private def shiftDown: Boolean = down("lshift", "rshift")

  /** Ctrl, or Cmd on macOS. */
  private def shortcutDown: Boolean = down("lctrl", "rctrl", "lgui", "rgui")

  /** Ctrl, or Alt (Option on macOS). */
  private def wordModifierDown: Boolean = down("lctrl", "rctrl", "lalt", "ralt")

  def apply(parent: Frame): EditBox = new EditBox(parent)

  def apply(parent: Frame, text: String, placeholder: String, maxLetters: Int): EditBox = {
    val box = new EditBox(parent)
    box.setMaxLetters(maxLetters)
    box.setText(text)
    box.setPlaceholder(placeholder)
    box
  }
}
