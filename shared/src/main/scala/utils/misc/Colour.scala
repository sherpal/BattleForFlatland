package utils.misc

trait Colour {

  def red: Int
  def green: Int
  def blue: Int
  def alpha: Double

  def intColour: Int = (red << 16) + (green << 8) + blue
  def rgb: String    = s"rgb($red, $green, $blue)"
  def rgba: String   = s"rgba($red, $green, $blue,$alpha)"

  def asRGBAColour: RGBAColour

}
