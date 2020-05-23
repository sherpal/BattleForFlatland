package utils.misc

import models.syntax.Pointed

import scala.language.implicitConversions
import scala.util.Random

final case class RGBColour(red: Int, green: Int, blue: Int) extends Colour {
  def rgba(alpha: Double): RGBAColour = RGBAColour(red, green, blue, alpha)
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

  implicit def asRGBA(rgb: RGBColour): RGBAColour = rgb.rgba(1.0)

}
