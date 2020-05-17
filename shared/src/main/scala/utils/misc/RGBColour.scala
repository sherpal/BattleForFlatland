package utils.misc

import models.syntax.Pointed

import scala.util.Random

final case class RGBColour(red: Int, green: Int, blue: Int) {

  def intColour: Int = (red << 16) + (green << 8) + blue

  def rgb: String = s"rgb($red, $green, $blue)"

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

}
