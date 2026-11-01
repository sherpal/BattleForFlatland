package bffclient.gui

/** A point of a region, as fractions of its width and height from its top left corner (y goes down,
  * like in LÖVE). Besides WoW's nine points, any fraction can be used, e.g. `Point(0.4, 0.5)` for a
  * column at 40% of a table's width.
  */
final case class Point(x: Double, y: Double)

object Point {
  val TopLeft     = Point(0, 0)
  val Top         = Point(0.5, 0)
  val TopRight    = Point(1, 0)
  val Left        = Point(0, 0.5)
  val Center      = Point(0.5, 0.5)
  val Right       = Point(1, 0.5)
  val BottomLeft  = Point(0, 1)
  val Bottom      = Point(0.5, 1)
  val BottomRight = Point(1, 1)
}

/** `point` of a region is at `relativePoint` of `relativeTo`, moved by (x, y). */
final case class Anchor(
    point: Point,
    relativeTo: Region,
    relativePoint: Point,
    x: Double,
    y: Double
)
