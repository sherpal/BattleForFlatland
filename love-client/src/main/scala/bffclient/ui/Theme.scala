package bffclient.ui

final case class Rect(x: Double, y: Double, width: Double, height: Double) {
  def right: Double  = x + width
  def bottom: Double = y + height

  def contains(px: Double, py: Double): Boolean  = px >= x && px < right && py >= y && py < bottom
  def contains(point: (Double, Double)): Boolean = contains(point._1, point._2)

  def inset(d: Double): Rect = Rect(x + d, y + d, width - 2 * d, height - 2 * d)
}

final case class Colour(r: Double, g: Double, b: Double, a: Double = 1.0) {
  def lighter(amount: Double): Colour =
    Colour(r + (1 - r) * amount, g + (1 - g) * amount, b + (1 - b) * amount, a)
  def withAlpha(alpha: Double): Colour = copy(a = alpha)
}

object Colour {
  def rgb(red: Int, green: Int, blue: Int): Colour =
    Colour(red / 255.0, green / 255.0, blue / 255.0)
}

object Theme {
  val background   = Colour.rgb(28, 30, 40)
  val panel        = Colour.rgb(40, 43, 56)
  val border       = Colour.rgb(80, 85, 105)
  val text         = Colour.rgb(235, 235, 240)
  val muted        = Colour.rgb(150, 155, 170)
  val field        = Colour.rgb(20, 21, 28)
  val button       = Colour.rgb(62, 66, 84)
  val primary      = Colour.rgb(40, 110, 200)
  val positive     = Colour.rgb(40, 150, 80)
  val negative     = Colour.rgb(190, 60, 60)
  val disabled     = Colour.rgb(50, 52, 62)
  val disabledText = Colour.rgb(105, 108, 120)

  val small = 13
  val body  = 16
  val large = 22
  val title = 34
}

enum ButtonStyle(val colour: Colour) {
  case Normal extends ButtonStyle(Theme.button)
  case Primary extends ButtonStyle(Theme.primary)
  case Positive extends ButtonStyle(Theme.positive)
  case Danger extends ButtonStyle(Theme.negative)
}
