package bffclient.gui

import scala.collection.mutable

import bffclient.love.{Font, Graphics, Image, Quad}
import bffclient.lua.ScalaLove
import bffclient.ui.{Colour, Theme}

/** The layers of a frame's regions, drawn in this order (WoW's draw layers). */
enum Layer {
  case Background, Border, Artwork, Overlay, Highlight
}

/** What a frame draws: [[Texture]]s and [[FontString]]s, created by [[Frame.createTexture]] and
  * [[Frame.createFontString]]. They are drawn with their frame, by layer, then by sub-layer, then
  * in the order they were created.
  */
sealed abstract class LayeredRegion(owner: Frame, private var _layer: Layer) extends Region {
  private var _subLayer = 0
  owner.addRegion(this)

  val parent: Option[Frame] = Some(owner)

  def effectiveScale: Double                = owner.effectiveScale
  private[gui] def scaleOwner: Frame | Null = owner
  private[gui] def localScale: Double       = 1.0

  def layer: Layer  = _layer
  def subLayer: Int = _subLayer
  def setDrawLayer(layer: Layer, subLayer: Int = 0): Unit = {
    _layer = layer
    _subLayer = subLayer
    owner.sortRegions()
  }

  private[gui] def draw(): Unit
}

/** A coloured rectangle (with rounded corners, or a circle when they are half its size), or an
  * image (tinted with the colour), stretched over the texture, possibly outlined.
  */
final class Texture private[gui] (owner: Frame, layer: Layer) extends LayeredRegion(owner, layer) {
  private var _colour       = Colour(1, 1, 1)
  private var image         = Option.empty[Image]
  private var part          = Option.empty[(Quad, Double, Double)]
  private var outlineColour = Option.empty[Colour]
  private var outlineWidth  = 1.0
  private var cornerRadius  = 0.0

  def colour: Colour                  = _colour
  def setColour(colour: Colour): Unit = _colour = colour
  def setImage(image: Image): Unit    = { this.image = Some(image); part = None }

  /** Only the part `quad` of the image, which is `width` x `height` pixels in the image. */
  def setImage(image: Image, quad: Quad, width: Double, height: Double): Unit = {
    this.image = Some(image)
    part = Some((quad, width, height))
  }

  def clearImage(): Unit = { image = None; part = None }

  /** A border inside the texture, in that colour (a transparent colour draws only the border). */
  def setOutline(colour: Option[Colour], width: Double = 1): Unit = {
    outlineColour = colour
    outlineWidth = width
  }

  /** Of the rectangle and of its outline (not of images). */
  def setCornerRadius(radius: Double): Unit = cornerRadius = radius

  private[gui] def draw(): Unit = {
    val r     = rect
    val alpha = owner.drawAlpha
    image match {
      case Some(img) =>
        Draw.colour(_colour, alpha)
        part match {
          case Some((quad, width, height)) =>
            Graphics.draw(img, quad, r.x, r.y, 0, r.width / width, r.height / height, 0, 0)
          case None =>
            Graphics.draw(
              img,
              r.x,
              r.y,
              0,
              r.width / img.getWidth(),
              r.height / img.getHeight(),
              0,
              0
            )
        }
      case None => Draw.fill(r, _colour, alpha, cornerRadius)
    }
    outlineColour match {
      case Some(colour) => Draw.outline(r, colour, alpha, cornerRadius, outlineWidth)
      case None => ()
    }
  }
}

enum JustifyH(val love: String) {
  case Left extends JustifyH("left")
  case Center extends JustifyH("center")
  case Right extends JustifyH("right")
}

enum JustifyV {
  case Top, Middle, Bottom
}

/** A text. Without a width from its anchors, it is as wide as its text; without a height, as high
  * as its lines. With word wrap, it wraps at its width.
  */
final class FontString private[gui] (owner: Frame, layer: Layer)
    extends LayeredRegion(owner, layer) {
  private var _text     = ""
  private var _size     = Theme.body
  private var _colour   = Theme.text
  private var _justifyH = JustifyH.Left
  private var _justifyV = JustifyV.Middle
  private var wordWrap  = false
  private var shadow    = Option.empty[Colour]

  private var wrappedText  = ""
  private var wrappedWidth = -1.0
  private var wrappedLines = 1

  def text: String                = _text
  def setText(text: String): Unit = if text != _text then { _text = text; layoutChanged() }

  def fontSize: Int                = _size
  def setFontSize(size: Int): Unit = if size != _size then { _size = size; layoutChanged() }

  def colour: Colour                  = _colour
  def setColour(colour: Colour): Unit = _colour = colour

  def setJustifyH(justify: JustifyH): Unit = _justifyH = justify
  def setJustifyV(justify: JustifyV): Unit = _justifyV = justify

  def setWordWrap(enabled: Boolean): Unit = if enabled != wordWrap then {
    wordWrap = enabled
    layoutChanged()
  }

  /** Drawn under the text, one pixel lower on the right. */
  def setShadow(colour: Option[Colour]): Unit = shadow = colour

  /** In a frame with a scale, texts are drawn with fonts of the scaled size (rather than scaled),
    * so that they stay sharp.
    */
  private def scale: Double = effectiveScale

  private var fontScale        = -1.0
  private var cachedFontSize   = -1
  private var cachedFont: Font = null

  def font: Font = {
    val scale = this.scale
    if scale != fontScale || _size != cachedFontSize then {
      fontScale = scale
      cachedFontSize = _size
      cachedFont = Fonts(math.max(1, math.round(_size * scale).toInt))
    }
    cachedFont
  }

  /** The width of the text on one line (the widest line, with line breaks). */
  def stringWidth: Double = font.getWidth(_text) / scale

  private def lineHeight: Double = font.getHeight() / scale

  /** The height of its lines, at its width (which does not depend on where it is). */
  def stringHeight: Double = lineCount(width) * lineHeight

  override protected def intrinsicWidth: Double  = stringWidth
  override protected def intrinsicHeight: Double = stringHeight

  private def lineCount(width: Double): Int =
    if !wordWrap then _text.count(_ == '\n') + 1
    else {
      if wrappedText != _text || wrappedWidth != width then {
        wrappedText = _text
        wrappedWidth = width
        wrappedLines =
          ScalaLove.fromLuaSequence(font.getWrap(_text, width * scale).lines).length max 1
      }
      wrappedLines
    }

  private[gui] def draw(): Unit = if _text.nonEmpty then {
    val r          = rect
    val scale      = this.scale
    val textHeight = lineCount(r.width) * lineHeight
    val y = _justifyV match {
      case JustifyV.Top => r.y
      case JustifyV.Middle => r.y + (r.height - textHeight) / 2
      case JustifyV.Bottom => r.bottom - textHeight
    }
    // Without word wrap, a text wider than the region overflows it, on the side of its alignment
    val limit = if wordWrap then r.width else math.max(r.width, stringWidth)
    val x = _justifyH match {
      case JustifyH.Left => r.x
      case JustifyH.Center => r.x + (r.width - limit) / 2
      case JustifyH.Right => r.right - limit
    }
    val alpha = owner.drawAlpha
    Graphics.setFont(font)
    // in the window's pixels, with the font of the scaled size
    if scale != 1 then { Graphics.push(); Graphics.scale(1 / scale) }
    def print(dx: Double): Unit = Graphics.printf(
      _text,
      math.round(x * scale).toDouble + dx,
      math.round(y * scale).toDouble + dx,
      limit * scale,
      _justifyH.love
    )
    for colour <- shadow do { Draw.colour(colour, alpha); print(1) }
    Draw.colour(_colour, alpha)
    print(0)
    if scale != 1 then Graphics.pop()
  }
}

/** The fonts, by size. */
object Fonts {
  private val fonts = mutable.Map.empty[Int, Font]

  def apply(size: Int): Font = fonts.getOrElseUpdate(size, Graphics.newFont(size))
}
