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
