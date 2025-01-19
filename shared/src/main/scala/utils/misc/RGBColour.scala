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
    colour  % 256
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
  val white        = RGBColour.fromIntColour(0xffffff)
  val red          = RGBColour.fromIntColour(0xff0000)
  val green        = RGBColour.fromIntColour(0x00ff00)
  val blue         = RGBColour.fromIntColour(0x0000ff)
  val yellow       = RGBColour.fromIntColour(0xffff00)
  val fuchsia      = RGBColour.fromIntColour(0xff00ff)
  val aqua         = RGBColour.fromIntColour(0x00ffff)
  val gray         = RGBColour.fromIntColour(0xc0c0c0)
  val orange       = RGBColour.fromIntColour(0xff9900)
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

  /** Creates an infinite [[LazyList]] of rotating colours. */
  def repeatedColours: LazyList[RGBColour] = LazyList.continually(someColours).flatten

}
