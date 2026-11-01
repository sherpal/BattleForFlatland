package bffclient.gui

import scala.collection.mutable

import bffclient.love.Graphics
import bffclient.ui.{Colour, Rect, Theme}

/** The strata of frames, drawn in this order (WoW's frame strata). A frame is in the stratum of its
  * parent unless it is given one.
  */
enum Strata {
  case Background, Low, Medium, High, Dialog, Fullscreen, FullscreenDialog, Tooltip
}

/** A kind of script, with the type of its handlers (WoW's "OnClick", "OnEnter", ...). */
final class Script[H] private[gui] (val name: String) {
  override def toString: String = name
}

object Script {

  /** When the frame becomes visible, or hidden (also when its parent does). */
  val OnShow: Script[() => Unit] = Script[() => Unit]("OnShow")
  val OnHide: Script[() => Unit] = Script[() => Unit]("OnHide")

  /** Every frame while the frame is visible, with the seconds since the previous one. */
  val OnUpdate: Script[Double => Unit] = Script[Double => Unit]("OnUpdate")

  /** The mouse enters or leaves the frame (which must have the mouse enabled). */
  val OnEnter: Script[() => Unit] = Script[() => Unit]("OnEnter")
  val OnLeave: Script[() => Unit] = Script[() => Unit]("OnLeave")

  /** With LÖVE's button numbers (1 left, 2 right, 3 middle, ...). */
  val OnMouseDown: Script[Int => Unit] = Script[Int => Unit]("OnMouseDown")
  val OnMouseUp: Script[Int => Unit]   = Script[Int => Unit]("OnMouseUp")

  /** A press and a release of the button over the same frame (for a [[Button]]: when enabled). */
  val OnClick: Script[Int => Unit] = Script[Int => Unit]("OnClick")

  /** How much the wheel turned (positive upwards), for frames with the mouse wheel enabled. */
  val OnMouseWheel: Script[Double => Unit] = Script[Double => Unit]("OnMouseWheel")

  /** A key (LÖVE scancode) pressed, for frames with the keyboard enabled (the topmost one gets it).
    */
  val OnKeyDown: Script[String => Unit] = Script[String => Unit]("OnKeyDown")

  /** [[EditBox]]es' */
  val OnEnterPressed: Script[() => Unit]    = Script[() => Unit]("OnEnterPressed")
  val OnTabPressed: Script[() => Unit]      = Script[() => Unit]("OnTabPressed")
  val OnEscapePressed: Script[() => Unit]   = Script[() => Unit]("OnEscapePressed")
  val OnTextChanged: Script[String => Unit] = Script[String => Unit]("OnTextChanged")
}

/** The background (and border) of a frame, drawn under its regions. */
final case class Backdrop(
    background: Colour,
    border: Option[Colour] = None,
    cornerRadius: Double = 0,
    /** A line along the top edge, as lit from above. */
    sheen: Option[Colour] = None
)

object Backdrop {
  val panel = Backdrop(Theme.panel, Some(Theme.border))
}

/** A frame, à la World of Warcraft: a [[Region]] with children (frames, textures, font strings)
  * that reacts to the inputs through scripts, e.g. `frame.setScript(Script.OnClick)(button =>
  * ...)`.
  *
  * Frames are drawn by stratum, then by level (by default, one more than their parent's), then in
  * the order of the tree. Each one draws its backdrop, then its regions by layer. The mouse goes to
  * the topmost frame under it with the mouse enabled.
  *
  * A frame is created in its parent: `Frame(parent)`. The root of every frame is the [[Gui]]'s
  * `root` (WoW's UIParent), which covers the window. A frame can have a scale ([[setScale]]), which
  * its descendants inherit: e.g. the HUD's frame, with the HUD's scale setting.
  */
class Frame private[gui] (private var _parent: Option[Frame]) extends Region {
  def this(parent: Frame) = this(Some(parent))

  _parent.foreach(_.children += this)

  private[gui] val children = mutable.ArrayBuffer.empty[Frame]
  private val regions       = mutable.ArrayBuffer.empty[LayeredRegion]
  private val scripts       = mutable.Map.empty[Script[?], List[Any]]

  private var explicitStrata = Option.empty[Strata]
  private var explicitLevel  = Option.empty[Int]
  private var _backdrop      = Option.empty[Backdrop]
  private var clipsChildren  = false
  private var alpha          = 1.0
  private var scale          = 1.0

  private var mouseEnabled       = false
  private var clickButtons       = Option.empty[Set[Int]]
  private var mouseWheelEnabled  = false
  private var keyboardEnabled    = false
  private var propagatesKeyboard = false

  def parent: Option[Frame] = _parent

  /** The [[Gui]] of its root (it fails once the frame, or an ancestor, was removed). */
  def gui: Gui = attachedGui.getOrElse(throw IllegalStateException(s"$this was removed"))

  /** The [[Gui]] of its root, unless it was removed (e.g. a screen's frame, after it was left). */
  def attachedGui: Option[Gui] = _parent.flatMap(_.attachedGui)

  /** Everything in it (sizes, offsets, texts, and its descendants) is drawn that much bigger, like
    * WoW's `SetScale`: anchored to its parent's edges, it covers the same part of the screen, with
    * fewer units in it.
    */
  def setScale(scale: Double): Unit = {
    require(scale > 0, s"The scale of $this must be positive")
    this.scale = scale
    Region.invalidate()
  }
  def getScale: Double = scale

  private var scaleCache      = 1.0
  private var scaleGeneration = -1

  // asked for by every anchor: cached like the positions
  private[gui] def scaleOwner: Frame | Null = _parent.orNull
  private[gui] def localScale: Double       = scale

  def effectiveScale: Double = {
    if scaleGeneration != Region.generation then {
      scaleCache = _parent match {
        case Some(parent) => scale * parent.effectiveScale
        case None => scale
      }
      scaleGeneration = Region.generation
    }
    scaleCache
  }

  /** Moves it (with its children) into another frame. */
  def setParent(frame: Frame): Unit = {
    _parent.foreach(_.children -= this)
    _parent = Some(frame)
    frame.children += this
    Region.invalidate()
  }

  /** Hides it and takes it out of its parent, for good. */
  def remove(): Unit = {
    hide()
    _parent.foreach(_.children -= this)
    _parent = None
    Region.invalidate()
  }

  def getChildren: Seq[Frame] = children.toSeq

  // --- Regions ---

  def createTexture(layer: Layer = Layer.Artwork): Texture = Texture(this, layer)

  def createTexture(colour: Colour, layer: Layer): Texture = {
    val texture = Texture(this, layer)
    texture.setColour(colour)
    texture
  }

  def createFontString(
      text: String = "",
      size: Int = Theme.body,
      colour: Colour = Theme.text,
      layer: Layer = Layer.Artwork
  ): FontString = {
    val fontString = FontString(this, layer)
    fontString.setText(text)
    fontString.setFontSize(size)
    fontString.setColour(colour)
    fontString
  }

  private[gui] def addRegion(region: LayeredRegion): Unit = { regions += region; sortRegions() }

  private[gui] def sortRegions(): Unit = {
    val sorted = regions.sortBy(region => (region.layer.ordinal, region.subLayer))
    regions.clear()
    regions ++= sorted
  }

  // --- Draw order ---

  def strata: Strata = explicitStrata match {
    case Some(strata) => strata
    case None =>
      _parent match {
        case Some(parent) => parent.strata
        case None => Strata.Medium
      }
  }
  def setFrameStrata(strata: Strata): Unit = explicitStrata = Some(strata)

  def frameLevel: Int = explicitLevel match {
    case Some(level) => level
    case None =>
      _parent match {
        case Some(parent) => parent.frameLevel + 1
        case None => 0
      }
  }
  def setFrameLevel(level: Int): Unit = explicitLevel = Some(level)

  // What drawing it needs, computed by the Gui from its parent's every frame (see `Gui.prepare`)
  private[gui] var drawKey   = 0.0
  private[gui] var drawScale = 1.0
  private[gui] var drawAlpha = 1.0
  private[gui] var drawClip  = Option.empty[Rect]

  private[gui] def strataOrdinal(parentStrata: Int): Int = explicitStrata match {
    case Some(strata) => strata.ordinal
    case None => parentStrata
  }

  private[gui] def levelFrom(parentLevel: Int): Int = explicitLevel match {
    case Some(level) => level
    case None => parentLevel + 1
  }

  def backdrop: Option[Backdrop]                    = _backdrop
  def setBackdrop(backdrop: Backdrop): Unit         = _backdrop = Some(backdrop)
  def setBackdrop(backdrop: Option[Backdrop]): Unit = _backdrop = backdrop

  /** Its regions and descendants are only drawn (and clicked) inside it. */
  def setClipsChildren(clips: Boolean): Unit = clipsChildren = clips

  /** Its opacity, and its descendants' (multiplied by its parent's). */
  def setAlpha(alpha: Double): Unit = this.alpha = math.max(0.0, math.min(1.0, alpha))
  def getAlpha: Double              = alpha
  def effectiveAlpha: Double        = alpha * _parent.fold(1.0)(_.effectiveAlpha)

  /** The bottom of the lowest of its shown regions and descendants (not itself), at least `top`. */
  def contentBottom: Double = {
    var bottom = top
    for region <- regions if region.isShown && region.isAnchored do
      bottom = math.max(bottom, region.bottom)
    for child <- children if child.isShown do {
      val childBottom =
        if child.isAnchored then child.bottom.max(child.contentBottom) else child.contentBottom
      bottom = math.max(bottom, childBottom * child.scale)
    }
    bottom
  }

  /** Where its own regions can be drawn, if somewhere in particular (in the window's pixels), from
    * its parent's.
    */
  private[gui] def clipFrom(parentClip: Option[Rect]): Option[Rect] =
    if clipsChildren then {
      val s      = drawScale
      val r      = rect
      val inside = Rect(r.x * s, r.y * s, r.width * s, r.height * s)
      parentClip match {
        case Some(clip) => Some(Frame.intersection(inside, clip))
        case None => Some(inside)
      }
    } else parentClip

  /** Whether the mouse, at (x, y) in the window's pixels, is over it (where it is not clipped, as
    * of the last frame).
    */
  private[gui] def containsWindowPoint(x: Double, y: Double): Boolean = {
    val s = drawScale
    isMouseOver(x / s, y / s) && (drawClip match {
      case Some(clip) => clip.contains(x, y)
      case None => true
    })
  }

  private[gui] def draw(): Unit = {
    _backdrop match {
      case Some(backdrop) =>
        val r      = rect
        val alpha  = drawAlpha
        val radius = backdrop.cornerRadius
        Draw.fill(r, backdrop.background, alpha, radius)
        backdrop.sheen match {
          case Some(sheen) =>
            Draw.colour(sheen, alpha)
            Graphics.line(r.x + radius, r.y + 1.5, r.right - radius, r.y + 1.5)
          case None => ()
        }
        backdrop.border match {
          case Some(border) => Draw.outline(r, border, alpha, radius)
          case None => ()
        }
      case None => ()
    }
    var i = 0
    while i < regions.length do {
      val region = regions(i)
      if region.isShown && region.isAnchored then region.draw()
      i += 1
    }
  }

  // --- Visibility ---

  override def show(): Unit = if !isShown then {
    super.show()
    if isVisible then fireVisibility(Script.OnShow)
  }

  override def hide(): Unit = if isShown then {
    val wasVisible = isVisible
    super.hide()
    if wasVisible then fireVisibility(Script.OnHide)
  }

  private def fireVisibility(script: Script[() => Unit]): Unit = {
    fire(script)(_())
    for child <- children.toList if child.isShown do child.fireVisibility(script)
  }

  // --- Inputs ---

  def enableMouse(enabled: Boolean): Unit = mouseEnabled = enabled
  def isMouseEnabled: Boolean             = mouseEnabled

  /** With the mouse enabled, it only takes these buttons (all of them by default): the others go to
    * what is under it, like with WoW's `RegisterForClicks`.
    */
  def registerForClicks(buttons: Int*): Unit = clickButtons = Some(buttons.toSet)

  private[gui] def takesButton(button: Int): Boolean =
    mouseEnabled && clickButtons.forall(_.contains(button))

  def enableMouseWheel(enabled: Boolean): Unit = mouseWheelEnabled = enabled
  def isMouseWheelEnabled: Boolean             = mouseWheelEnabled

  /** It gets the keys pressed (when no text is typed in an [[EditBox]]), before the frames under
    * it, which only get them too if it propagates them.
    */
  def enableKeyboard(enabled: Boolean): Unit = keyboardEnabled = enabled
  def isKeyboardEnabled: Boolean             = keyboardEnabled

  def setPropagateKeyboardInput(propagate: Boolean): Unit = propagatesKeyboard = propagate
  def propagatesKeyboardInput: Boolean                    = propagatesKeyboard

  /** What the frame does itself, before its scripts (e.g. a [[Button]] is only clicked when
    * enabled).
    */
  protected[gui] def mouseDown(button: Int): Unit    = fire(Script.OnMouseDown)(_(button))
  protected[gui] def mouseUp(button: Int): Unit      = fire(Script.OnMouseUp)(_(button))
  protected[gui] def click(button: Int): Unit        = fire(Script.OnClick)(_(button))
  protected[gui] def mouseWheel(delta: Double): Unit = fire(Script.OnMouseWheel)(_(delta))
  protected[gui] def keyDown(scancode: String): Unit = fire(Script.OnKeyDown)(_(scancode))

  /** Called every frame before drawing, while visible: widgets update their regions there (e.g. a
    * button's highlight).
    */
  protected[gui] def refresh(gui: Gui): Unit = ()

  // --- Scripts ---

  /** Replaces the handlers of the script. */
  def setScript[H](script: Script[H])(handler: H): Unit = scripts(script) = List(handler)

  /** Adds a handler to the script, called after the others. */
  def hookScript[H](script: Script[H])(handler: H): Unit =
    scripts(script) = scripts.getOrElse(script, Nil) :+ handler

  def clearScript(script: Script[?]): Unit = scripts -= script

  def hasScript(script: Script[?]): Boolean = scripts.contains(script)

  /** Calls the handlers of the script with `call`, e.g. `fire(Script.OnClick)(_(button))`. */
  def fire[H](script: Script[H])(call: H => Unit): Unit = if scripts.nonEmpty then {
    scripts.get(script) match {
      case Some(handlers) => handlers.foreach(handler => call(handler.asInstanceOf[H]))
      case None => ()
    }
  }

  private[gui] def hasScripts: Boolean = scripts.nonEmpty
}

object Frame {
  private[gui] def intersection(a: Rect, b: Rect): Rect = {
    val x = math.max(a.x, b.x)
    val y = math.max(a.y, b.y)
    Rect(
      x,
      y,
      math.max(0.0, math.min(a.right, b.right) - x),
      math.max(0.0, math.min(a.bottom, b.bottom) - y)
    )
  }
}

/** The root of a [[Gui]]'s frames: it covers the window. */
final class UIParent private[gui] (val owner: Gui) extends Frame(None) {
  override def gui: Gui                 = owner
  override def attachedGui: Option[Gui] = Some(owner)
  override def isVisible: Boolean       = isShown

  override protected def solve(isHorizontal: Boolean): Unit = {
    // in its units: the window's size, divided by its scale
    val screen = Graphics.getDimensions()
    val size   = if isHorizontal then screen.width.toDouble else screen.height.toDouble
    solved(isHorizontal, 0.0, size / effectiveScale)
  }
}
