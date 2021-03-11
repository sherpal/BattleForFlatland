package utils.misc

import models.syntax.Pointed

import scala.language.implicitConversions
import scala.util.Random

final case class RGBColour(red: Int, green: Int, blue: Int) extends Colour {
  def alpha: Double                        = 1
  def withAlpha(alpha: Double): RGBAColour = RGBAColour(red, green, blue, alpha)
  def withoutAlpha: RGBColour              = this
  def asRGBAColour: RGBAColour             = withAlpha(1.0)
}

object RGBColour {
  def fromIntColour(colour: Int): RGBColour = RGBColour(
    colour >> 16,
    (colour % (256 << 8)) / 256,
    colour % 256
  )
  import io.circe._
  import io.circe.generic.semiauto._
  implicit val fooDecoder: Decoder[RGBColour] = deriveDecoder[RGBColour]
  implicit val fooEncoder: Encoder[RGBColour] = deriveEncoder[RGBColour]

  implicit def pointed: Pointed[RGBColour] = Pointed.factory(
    RGBColour(Random.nextInt(255), Random.nextInt(255), Random.nextInt(255))
  )

  implicit def asRGBA(rgb: RGBColour): RGBAColour = rgb.withAlpha(1.0)

  val black        = RGBColour.fromIntColour(0)
  val white        = RGBColour.fromIntColour(0xFFFFFF)
  val red          = RGBColour.fromIntColour(0xFF0000)
  val green        = RGBColour.fromIntColour(0x00FF00)
  val blue         = RGBColour.fromIntColour(0x0000FF)
  val yellow       = RGBColour.fromIntColour(0xFFFF00)
  val fuchsia      = RGBColour.fromIntColour(0xFF00FF)
  val aqua         = RGBColour.fromIntColour(0x00FFFF)
  val gray         = RGBColour.fromIntColour(0xC0C0C0)
  val orange       = RGBColour.fromIntColour(0xFF9900)
  val brown        = RGBColour.fromIntColour(0x996633)
  val lightGreen   = RGBColour.fromIntColour(0x00cc99)
  val electricBlue = RGBColour.fromIntColour(0x6666ff)

  val someColours: Vector[RGBColour] = Vector(
    red,
    green,
    blue,
    yellow,
    fuchsia,
    aqua,
    orange,
    brown,
    lightGreen,
    electricBlue
  )

  /** Creates an inifite [[LazyList]] of rotating colours. */
  def repeatedColours: LazyList[RGBColour] = LazyList.continually(someColours).flatten

}
