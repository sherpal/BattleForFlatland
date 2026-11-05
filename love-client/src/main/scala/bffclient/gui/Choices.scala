package bffclient.gui

import bffclient.ui.{ButtonStyle, Theme}

/** A choice among options, which fires its own `OnValueChanged` (typed by its options) when the
  * player chooses one: `selector.setScript(selector.OnValueChanged)(option => ...)`.
  */
sealed abstract class Choice[A](parentFrame: Frame, show: A => String) extends Frame(parentFrame) {
  val OnValueChanged: Script[A => Unit] = Script("OnValueChanged")

  protected var options = Seq.empty[A]
  protected var current = Option.empty[A]
  protected var enabled = true

  def value: Option[A] = current

  /** Shows the options and the current one (without firing `OnValueChanged`). */
  def setOptions(options: Seq[A], current: Option[A]): Unit = {
    val changedOptions = options != this.options
    this.options = options
    this.current = current
    updated(changedOptions)
  }

  def setEnabled(enabled: Boolean): Unit = {
    this.enabled = enabled
    updated(changedOptions = false)
  }

  protected def updated(changedOptions: Boolean): Unit

  protected def choose(option: A): Unit = {
    current = Some(option)
    updated(changedOptions = false)
    fire(OnValueChanged)(_(option))
  }

  protected def label(option: A): String = show(option)
}

/** `< value >`: the arrows go through the options. */
class Selector[A](parentFrame: Frame, show: A => String = (a: A) => a.toString)
    extends Choice[A](parentFrame, show) {
  private val previous = Button(this, "<")
  private val next     = Button(this, ">")
  private val middle   = createTexture(Theme.field, Layer.Background)
  private val text     = createFontString()

  previous.setPoint(Point.TopLeft, this, Point.TopLeft)
  previous.setPoint(Point.BottomLeft, this, Point.BottomLeft)
  next.setPoint(Point.TopRight, this, Point.TopRight)
  next.setPoint(Point.BottomRight, this, Point.BottomRight)
  middle.setPoint(Point.TopLeft, previous, Point.TopRight)
  middle.setPoint(Point.BottomRight, next, Point.BottomLeft)
  text.setAllPoints(middle)
  text.setJustifyH(JustifyH.Center)
  previous.setScript(Script.OnClick)(_ => step(-1))
  next.setScript(Script.OnClick)(_ => step(1))

  private def step(by: Int): Unit = if options.nonEmpty then {
    val index = current.fold(0)(options.indexOf(_))
    choose(options((index + by + options.size) % options.size))
  }

  protected def updated(changedOptions: Boolean): Unit = {
    text.setText(current.fold("")(label))
    text.setColour(if enabled then Theme.text else Theme.disabledText)
    previous.setEnabled(enabled && options.size > 1)
    next.setEnabled(enabled && options.size > 1)
  }

  override protected[gui] def refresh(gui: Gui): Unit =
    // square arrows
    if previous.width != height then {
      previous.setWidth(height)
      next.setWidth(height)
    }
}

/** Mutually exclusive options side by side, as buttons; the current one is highlighted. */
class Segmented[A](parentFrame: Frame, show: A => String = (a: A) => a.toString)
    extends Choice[A](parentFrame, show) {
  private var buttons = Vector.empty[Button]

  protected def updated(changedOptions: Boolean): Unit = {
    if changedOptions then {
      buttons.foreach(_.remove())
      buttons = options.zipWithIndex.map { (option, i) =>
        val button = Button(this, label(option))
        // each one a fraction of the width
        button.setPoint(Point.TopLeft, this, Point(i.toDouble / options.size, 0))
        button.setPoint(Point.BottomRight, this, Point((i + 1.0) / options.size, 1), -2, 0)
        button.setScript(Script.OnClick)(_ => choose(option))
        button
      }.toVector
    }
    for (button, option) <- buttons.zip(options) do {
      button.setStyle(if current.contains(option) then ButtonStyle.Primary else ButtonStyle.Normal)
      button.setEnabled(enabled)
    }
  }
}

/** A select: a field with the current option, which opens the list of the options under it, over
  * everything. Choosing one (or clicking elsewhere, or Escape) closes it.
  */
class Dropdown[A](parentFrame: Frame, show: A => String = (a: A) => a.toString)
    extends Choice[A](parentFrame, show) {
  private val field = Button(this)
  field.setAllPoints(this)
  field.setStyle(ButtonStyle.Normal)
  field.fontString.setJustifyH(JustifyH.Left)
  field.fontString.setPoint(Point.TopLeft, field, Point.TopLeft, 12, 0)
  field.fontString.setPoint(Point.BottomRight, field, Point.BottomRight, -28, 0)
  // A small triangle pointing down (the font has no glyph for it): rows narrowing downwards
  private val arrow = (0 until 5).map { row =>
    val part = field.createTexture(Theme.muted, Layer.Overlay)
    part.setSize(9 - 2 * row, 1)
    part.setPoint(Point.Top, field, Point.Right, -14, row - 2)
    part
  }
  field.setScript(Script.OnClick)(_ => if list.isDefined then close() else open())

  /** While open: a frame over the whole window (a click on it closes the list), and the list. */
  private var list = Option.empty[Frame]

  hookScript(Script.OnHide)(() => close())

  private def open(): Unit = if enabled && options.nonEmpty then {
    val catcher = Frame(gui.root)
    catcher.setAllPoints()
    catcher.setFrameStrata(Strata.FullscreenDialog)
    catcher.enableMouse(true)
    catcher.enableKeyboard(true)
    catcher.setScript(Script.OnClick)(_ => close())
    catcher.setScript(Script.OnKeyDown)(scancode => if scancode == "escape" then close())

    val panel = Frame(catcher)
    panel.setBackdrop(Backdrop(Theme.panel, Some(Theme.border), 4))
    panel.setPoint(Point.TopLeft, this, Point.BottomLeft, 0, 2)
    panel.setPoint(Point.TopRight, this, Point.BottomRight, 0, 2)
    panel.setHeight(options.size * height + 4)
    panel.enableMouse(true) // its gaps do not close it
    for (option, i) <- options.zipWithIndex do {
      val button = Button(panel, label(option))
      button.setPoint(Point.TopLeft, panel, Point.TopLeft, 2, 2 + i * height)
      button.setPoint(Point.TopRight, panel, Point.TopRight, -2, 2 + i * height)
      button.setHeight(height)
      button.setStyle(if current.contains(option) then ButtonStyle.Primary else ButtonStyle.Normal)
      button.fontString.setJustifyH(JustifyH.Left)
      button.fontString.setPoint(Point.TopLeft, button, Point.TopLeft, 10, 0)
      button.fontString.setPoint(Point.BottomRight, button, Point.BottomRight, -10, 0)
      button.setScript(Script.OnClick) { _ =>
        close()
        choose(option)
      }
    }
    list = Some(catcher)
  }

  /** Closes the list, if open. */
  def close(): Unit = {
    list.foreach(_.remove())
    list = None
  }

  protected def updated(changedOptions: Boolean): Unit = {
    field.setText(current.fold("")(label))
    field.setEnabled(enabled)
    arrow.foreach(_.setColour(if enabled then Theme.muted else Theme.disabledText))
    if changedOptions || !enabled then close()
  }
}
