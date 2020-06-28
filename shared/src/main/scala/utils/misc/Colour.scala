package utils.misc

trait Colour {

  def red: Int
  def green: Int
  def blue: Int
  def alpha: Double

  def withAlpha(newAlpha: Double): RGBAColour
  def withoutAlpha: RGBColour

  def intColour: Int = (red << 16) + (green << 8) + blue
  def rgb: String    = s"rgb($red, $green, $blue)"
  def rgba: String   = s"rgba($red, $green, $blue,$alpha)"

  def luma: Int         = (0.2126 * red + 0.7152 * green + 0.0722 * blue).toInt
  def isBright: Boolean = luma > 128

  def matchingTextColour: RGBColour = if (isBright) RGBColour.black else RGBColour.white

  def asRGBAColour: RGBAColour

}
