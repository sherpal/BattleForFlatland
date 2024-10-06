package assets.fonts

import org.scalajs.dom
import zio.*

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

@JSGlobal()
@js.native
private[fonts] class FontFace(val family: String, fontUrl: String) extends js.Object {
  def load(): js.Promise[js.Object] = js.native
}

@js.native
trait FontFaceSet extends js.Object {

  def add(fontFace: FontFace): Unit = js.native

  def load(font: String): js.Promise[js.Array[FontFace]] = js.native

}

extension (document: dom.Document) {
  def fonts: FontFaceSet = document.asInstanceOf[js.Dynamic].fonts.asInstanceOf[FontFaceSet]
}

val quicksand = FontFace(
  "Quicksand",
  "url('/static/assets/in-game/gui/fonts/Quicksand-Bold.ttf') format('truetype')"
)

val alphabet = {
  val letters      = ('a'.toByte.toInt to 'z'.toByte.toInt).map(_.toByte.toChar).toVector
  val upperLetters = letters.map(_.toUpper)
  val digits       = (0 to 9).map(_.toString.head).toVector
  val symbols      = Vector('.', '?', '!', ' ', '#', '>', '<', ':', '/', '€')
  Vector(
    letters,
    upperLetters,
    digits,
    symbols
  ).flatten
}

def createGlyphFontData(
    fontFace: FontFace,
    size: Int,
    color: String,
    alphabet: Vector[Char]
): ZIO[Any, Throwable, (String, dom.ImageData, GlyphInfo)] =

  val font        = fontFace.family
  val contextFont = s"${size}px $font"

  dom.document.fonts.add(fontFace)
  ZIO
    .fromPromiseJS(
      dom.document.fonts
        .load(contextFont)
    )
    .zipLeft(Clock.sleep(100.millis))
    .map { _ =>
      val horizontalPixelsBetweenChars = 0
      val verticalPixeslBetweenChars   = 0

      val canvas = dom.document.createElement("canvas").asInstanceOf[dom.HTMLCanvasElement]

      canvas.width = 1000
      canvas.height = 2 * size
      val context = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

      context.font = contextFont

      val rowSize = math.sqrt(alphabet.length).toInt.max(1)

      val rows = alphabet.grouped(rowSize).toVector

      case class Size(width: Int, height: Int)

      val alphabetMetrics = context.measureText(alphabet.mkString)
      val baselineOffset  = math.abs(alphabetMetrics.actualBoundingBoxAscent)
      val textHeight      = alphabetMetrics.intHeight

      val glyphSizes = alphabet.map { char =>
        val metrics = context.measureText(char.toString)
        char -> Size(metrics.intWidth, textHeight)
      }.toMap

      val cellWidth  = glyphSizes.values.map(_.width).max
      val cellHeight = glyphSizes.values.map(_.height).max

      val imageSize = Size(
        rows
          .map(_.length)
          .max * (cellWidth + horizontalPixelsBetweenChars) + horizontalPixelsBetweenChars,
        rows.length * (cellHeight + verticalPixeslBetweenChars) + verticalPixeslBetweenChars
      )
      canvas.width = imageSize.width
      canvas.height = imageSize.height

      val glyphPositions = for {
        (row, rowIndex)  <- rows.zipWithIndex
        (char, colIndex) <- row.zipWithIndex
        size  = glyphSizes(char)
        x     = colIndex * (cellWidth + horizontalPixelsBetweenChars) + horizontalPixelsBetweenChars
        y     = rowIndex * (cellHeight + verticalPixeslBetweenChars) + verticalPixeslBetweenChars
        width = size.width
        height = size.height
      } yield GlyphInfo.GlyphPosition(char, x, y, width, height)

      context.fillStyle = color
      context.font = contextFont
      glyphPositions.foreach { position =>
        context.fillText(
          position.char.toString,
          position.x.toDouble,
          position.y.toDouble + baselineOffset
        )
      }

      (
        canvas.toDataURL("png"),
        context.getImageData(0, 0, imageSize.width, imageSize.height),
        GlyphInfo(
          imageSize.width,
          imageSize.height,
          size,
          color,
          glyphPositions
        )
      )

    }

extension (metrics: dom.TextMetrics) {
  def actualBoundingBoxAscent: Double =
    metrics.asInstanceOf[js.Dynamic].actualBoundingBoxAscent.asInstanceOf[Double]
  def actualBoundingBoxDescent: Double =
    metrics.asInstanceOf[js.Dynamic].actualBoundingBoxDescent.asInstanceOf[Double]

  def height: Double = metrics.actualBoundingBoxAscent + metrics.actualBoundingBoxDescent

  def intHeight: Int = math.ceil(metrics.height).toInt
  def intWidth: Int  = math.ceil(metrics.width).toInt
}
