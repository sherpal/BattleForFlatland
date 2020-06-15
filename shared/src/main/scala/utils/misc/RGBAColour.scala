package utils.misc

final case class RGBAColour(red: Int, green: Int, blue: Int, alpha: Double) extends Colour {

  def removeAlpha: RGBColour = RGBColour(red, green, blue)

  def asRGBAColour: RGBAColour = this

}
