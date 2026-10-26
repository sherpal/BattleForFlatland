package bffclient.ui

import scala.collection.mutable

import bffclient.love.{Font, Graphics, Mouse, Timer}
import bffclient.lua.ScalaLove

final case class Rect(x: Double, y: Double, width: Double, height: Double) {
  def right: Double  = x + width
  def bottom: Double = y + height

  def contains(px: Double, py: Double): Boolean  = px >= x && px < right && py >= y && py < bottom
  def contains(point: (Double, Double)): Boolean = contains(point._1, point._2)

  def inset(d: Double): Rect = Rect(x + d, y + d, width - 2 * d, height - 2 * d)
}

final case class Colour(r: Double, g: Double, b: Double, a: Double = 1.0) {
  def lighter(amount: Double): Colour =
    Colour(r + (1 - r) * amount, g + (1 - g) * amount, b + (1 - b) * amount, a)
  def withAlpha(alpha: Double): Colour = copy(a = alpha)
}

object Colour {
  def rgb(red: Int, green: Int, blue: Int): Colour =
    Colour(red / 255.0, green / 255.0, blue / 255.0)
}

object Theme {
  val background   = Colour.rgb(28, 30, 40)
  val panel        = Colour.rgb(40, 43, 56)
  val border       = Colour.rgb(80, 85, 105)
  val text         = Colour.rgb(235, 235, 240)
  val muted        = Colour.rgb(150, 155, 170)
  val field        = Colour.rgb(20, 21, 28)
  val button       = Colour.rgb(62, 66, 84)
  val primary      = Colour.rgb(40, 110, 200)
  val positive     = Colour.rgb(40, 150, 80)
  val negative     = Colour.rgb(190, 60, 60)
  val disabled     = Colour.rgb(50, 52, 62)
  val disabledText = Colour.rgb(105, 108, 120)

  val small = 13
  val body  = 16
  val large = 22
  val title = 34
}

enum ButtonStyle(val colour: Colour) {
  case Normal extends ButtonStyle(Theme.button)
  case Primary extends ButtonStyle(Theme.primary)
  case Positive extends ButtonStyle(Theme.positive)
  case Danger extends ButtonStyle(Theme.negative)
}

/** The text of a text field. The instance identifies its field, e.g. for the keyboard focus. */
final class TextInput(var text: String = "", val maxLength: Int = 32)

/** A small immediate mode UI on love.graphics.
  *
  * Widgets are drawn and handled in the same call, every frame: `if ui.button("Join", rect) then
  * join()`. LÖVE's input callbacks are forwarded to [[mousePressed]], [[mouseReleased]],
  * [[textInput]] and [[keyPressed]]; they are consumed by the widgets of the next [[frame]].
  *
  * A click is a press and a release of the left button inside the same widget.
  */
final class Ui {
  private var mouse: (Double, Double)              = (0, 0)
  private var pressedAt: Option[(Double, Double)]  = None
  private var pressedThisFrame                     = false
  private val buttons                              = mutable.ArrayBuffer.empty[Int]
  private var wheelY                               = 0.0
  private var inputEnabled                         = true
  private var releasedAt: Option[(Double, Double)] = None
  private val typed                                = new StringBuilder
  private val keys                                 = mutable.ArrayBuffer.empty[String]
  private var focus: Option[TextInput]             = None
  private var focusClaimed                         = false
  private val fonts                                = mutable.Map.empty[Int, Font]

  def mousePressed(x: Double, y: Double, button: Int): Unit = {
    buttons += button
    if button == 1 then { pressedAt = Some((x, y)); pressedThisFrame = true }
  }

  def wheelMoved(dy: Double): Unit = wheelY += dy

  def mouseReleased(x: Double, y: Double, button: Int): Unit =
    if button == 1 then releasedAt = Some((x, y))

  def textInput(text: String): Unit = typed ++= text

  def keyPressed(scancode: String): Unit = keys += scancode

  /** Whether the key was pressed since the last frame (and not used by a text field). */
  def wasPressed(scancode: String): Boolean = inputEnabled && keys.contains(scancode)

  /** The keys (scancodes) pressed since the last frame, and not used by a text field. */
  def pressedKeys: Seq[String] = if inputEnabled then keys.toSeq else Seq.empty

  /** The mouse buttons (LÖVE's numbers: 1 left, 2 right, 3 middle, ...) pressed since the last
    * frame.
    */
  def pressedButtons: Seq[Int] = if inputEnabled then buttons.toSeq else Seq.empty

  /** How much the mouse wheel turned since the last frame (positive upwards). */
  def wheel: Double = if inputEnabled then wheelY else 0

  /** Draws widgets which ignore the inputs (e.g. under a dialog). */
  def inert(widgets: => Unit): Unit = {
    val previous = inputEnabled
    inputEnabled = false
    widgets
    inputEnabled = previous
  }

  /** Draws only inside `rect` (e.g. for a scrolling area). */
  def clipped(rect: Rect)(widgets: => Unit): Unit = {
    Graphics.setScissor(rect.x, rect.y, math.max(0.0, rect.width), math.max(0.0, rect.height))
    widgets
    Graphics.setScissor()
  }

  def focused(input: TextInput): Boolean = focus.contains(input)
  def setFocus(input: TextInput): Unit   = focus = Some(input)

  /** Draws (and handles) the widgets of one frame. The font is restored afterwards, so that the UI
    * can be drawn over something else (e.g. the game).
    */
  def frame(widgets: => Unit): Unit = {
    val position = Mouse.getPosition()
    mouse = (position.x, position.y)
    focusClaimed = false
    val previousFont = Graphics.getFont()
    widgets
    Graphics.setFont(previousFont)
    if pressedThisFrame && !focusClaimed then focus = None
    if releasedAt.isDefined then pressedAt = None
    releasedAt = None
    pressedThisFrame = false
    buttons.clear()
    wheelY = 0
    typed.clear()
    keys.clear()
  }

  // --- Drawing helpers ---

  def font(size: Int): Font = fonts.getOrElseUpdate(size, Graphics.newFont(size))

  def setColour(colour: Colour): Unit = Graphics.setColor(colour.r, colour.g, colour.b, colour.a)

  def fill(rect: Rect, colour: Colour): Unit = {
    setColour(colour)
    Graphics.rectangle("fill", rect.x, rect.y, rect.width, rect.height)
  }

  def outline(rect: Rect, colour: Colour): Unit = {
    setColour(colour)
    Graphics.setLineWidth(1)
    Graphics.rectangle("line", rect.x + 0.5, rect.y + 0.5, rect.width - 1, rect.height - 1)
  }

  def panel(rect: Rect): Unit = { fill(rect, Theme.panel); outline(rect, Theme.border) }

  def textWidth(text: String, size: Int = Theme.body): Double = font(size).getWidth(text)

  def text(
      text: String,
      x: Double,
      y: Double,
      size: Int = Theme.body,
      colour: Colour = Theme.text
  ): Unit = {
    Graphics.setFont(font(size))
    setColour(colour)
    Graphics.print(text, math.round(x).toDouble, math.round(y).toDouble)
  }

  /** Text centred vertically in `rect`, aligned "left", "center" or "right". */
  def textIn(
      text: String,
      rect: Rect,
      size: Int = Theme.body,
      colour: Colour = Theme.text,
      align: String = "center"
  ): Unit = {
    val f = font(size)
    Graphics.setFont(f)
    setColour(colour)
    Graphics.printf(
      text,
      math.round(rect.x).toDouble,
      math.round(rect.y + (rect.height - f.getHeight()) / 2).toDouble,
      rect.width,
      align
    )
  }

  /** Text wrapped at `width`; returns its height. */
  def paragraph(
      text: String,
      x: Double,
      y: Double,
      width: Double,
      size: Int = Theme.body,
      colour: Colour = Theme.text
  ): Double = {
    val f = font(size)
    Graphics.setFont(f)
    setColour(colour)
    Graphics.printf(text, math.round(x).toDouble, math.round(y).toDouble, width, "left")
    ScalaLove.fromLuaSequence(f.getWrap(text, width).lines).length * f.getHeight()
  }

  // --- Widgets ---

  def hovered(rect: Rect): Boolean = inputEnabled && rect.contains(mouse)

  private def clicked(rect: Rect): Boolean =
    inputEnabled && releasedAt.exists(rect.contains) && pressedAt.exists(rect.contains)

  private def held(rect: Rect): Boolean =
    inputEnabled && pressedAt.exists(rect.contains) && rect.contains(mouse)

  /** Returns whether the button was clicked. */
  def button(
      label: String,
      rect: Rect,
      enabled: Boolean = true,
      style: ButtonStyle = ButtonStyle.Normal
  ): Boolean =
    if !enabled then {
      fill(rect, Theme.disabled)
      textIn(label, rect, colour = Theme.disabledText)
      false
    } else {
      val colour =
        if held(rect) then style.colour.lighter(0.3)
        else if hovered(rect) then style.colour.lighter(0.15)
        else style.colour
      fill(rect, colour)
      textIn(label, rect)
      clicked(rect)
    }

  /** A single line text field. Returns whether Enter was pressed in it. */
  def textField(
      input: TextInput,
      rect: Rect,
      placeholder: String = "",
      enabled: Boolean = true
  ): Boolean = {
    if enabled && inputEnabled && pressedThisFrame && pressedAt.exists(rect.contains) then {
      focus = Some(input); focusClaimed = true
    }
    val hasFocus  = enabled && inputEnabled && focused(input)
    var submitted = false
    if hasFocus then {
      if typed.nonEmpty then input.text = (input.text + typed.toString).take(input.maxLength)
      keys.foreach {
        case "backspace" => input.text = Ui.dropLastCharacter(input.text)
        case "return" | "kpenter" => submitted = true
        case _ => ()
      }
      keys.filterInPlace(key => key != "backspace" && key != "return" && key != "kpenter")
    }
    fill(rect, Theme.field)
    outline(rect, if hasFocus then Theme.primary.lighter(0.3) else Theme.border)
    val inner = Rect(rect.x + 8, rect.y, rect.width - 16, rect.height)
    if input.text.isEmpty && !hasFocus then
      textIn(placeholder, inner, colour = Theme.muted, align = "left")
    else
      textIn(
        input.text,
        inner,
        colour = if enabled then Theme.text else Theme.disabledText,
        align = "left"
      )
    if hasFocus && (Timer.getTime() * 2).toInt % 2 == 0 then {
      val cursorX = inner.x + textWidth(input.text) + 1
      fill(Rect(cursorX, rect.y + 7, 2, rect.height - 14), Theme.text)
    }
    submitted
  }

  /** A check box followed by its label. Returns whether it was clicked. */
  def checkbox(
      checked: Boolean,
      label: String,
      x: Double,
      y: Double,
      enabled: Boolean = true
  ): Boolean = {
    val box  = Rect(x, y, 22, 22)
    val area = Rect(x, y, 30 + textWidth(label), 22)
    fill(box, Theme.field)
    outline(box, if enabled && hovered(area) then Theme.primary.lighter(0.3) else Theme.border)
    if checked then
      fill(box.inset(5), if enabled then Theme.positive.lighter(0.2) else Theme.disabledText)
    text(label, x + 30, y + 2, colour = if enabled then Theme.text else Theme.disabledText)
    enabled && clicked(area)
  }

  /** `< value >`: returns the option chosen with the arrows, if any. */
  def selector[A](
      options: Seq[A],
      current: A,
      rect: Rect,
      show: A => String = (a: A) => a.toString,
      enabled: Boolean = true
  ): Option[A] = {
    val index    = options.indexOf(current)
    val previous =
      button("<", Rect(rect.x, rect.y, rect.height, rect.height), enabled && options.size > 1)
    val next = button(
      ">",
      Rect(rect.right - rect.height, rect.y, rect.height, rect.height),
      enabled && options.size > 1
    )
    val middle = Rect(rect.x + rect.height, rect.y, rect.width - 2 * rect.height, rect.height)
    fill(middle, Theme.field)
    textIn(show(current), middle, colour = if enabled then Theme.text else Theme.disabledText)
    if options.isEmpty then None
    else if previous then Some(options((index - 1 + options.size) % options.size))
    else if next then Some(options((index + 1) % options.size))
    else None
  }

  /** Mutually exclusive options side by side. Returns the option clicked, if any. */
  def segmented[A](
      options: Seq[A],
      current: Option[A],
      rect: Rect,
      show: A => String = (a: A) => a.toString,
      enabled: Boolean = true
  ): Option[A] = {
    val width  = rect.width / options.size
    var chosen = Option.empty[A]
    for (option, i) <- options.zipWithIndex do {
      val style = if current.contains(option) then ButtonStyle.Primary else ButtonStyle.Normal
      if
        button(
          show(option),
          Rect(rect.x + i * width, rect.y, width - 2, rect.height),
          enabled,
          style
        )
      then
        chosen = Some(option)
    }
    chosen
  }

  /** A colour square. Returns whether it was clicked. */
  def swatch(
      colour: Colour,
      rect: Rect,
      selected: Boolean = false,
      enabled: Boolean = true
  ): Boolean = {
    fill(rect, colour)
    if selected then { outline(rect, Theme.text); outline(rect.inset(1), Theme.text) }
    else if enabled && hovered(rect) then outline(rect, Theme.text.withAlpha(0.6))
    enabled && clicked(rect)
  }

  /** "Loading..." with moving dots. */
  def loading(message: String, x: Double, y: Double): Unit =
    text(message + "." * ((Timer.getTime() * 3).toInt % 4), x, y, colour = Theme.muted)
}

object Ui {

  /** Removes the last (UTF-8) character: LÖVE's strings are bytes. */
  def dropLastCharacter(text: String): String = {
    var end = text.length - 1
    while end > 0 && (text.charAt(end) & 0xc0) == 0x80 do end -= 1
    text.substring(0, math.max(end, 0))
  }
}
