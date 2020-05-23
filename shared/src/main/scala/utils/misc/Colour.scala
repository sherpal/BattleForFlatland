package utils.misc

trait Colour {

  def red: Int
  def green: Int
  def blue: Int

  def intColour: Int = (red << 16) + (green << 8) + blue
  def rgb: String    = s"rgb($red, $green, $blue)"

}
