package game.ui

import indigo.{Point, Rectangle}

sealed trait AnchorPoint {
  def pointRelativeTo(rectangle: Rectangle): Point =
    this match {
      case AnchorPoint.TopLeft      => Point.zero
      case AnchorPoint.TopCenter    => Point(-rectangle.size.width / 2, 0)
      case AnchorPoint.TopRight     => Point(-rectangle.size.width, 0)
      case AnchorPoint.CenterLeft   => Point(0, -rectangle.size.height / 2)
      case AnchorPoint.Center       => Point(-rectangle.size.width / 2, -rectangle.size.height / 2)
      case AnchorPoint.CenterRight  => Point(-rectangle.size.width, -rectangle.size.height / 2)
      case AnchorPoint.BottomLeft   => Point(0, -rectangle.size.height)
      case AnchorPoint.BottomCenter => Point(-rectangle.size.width / 2, -rectangle.size.height)
      case AnchorPoint.BottomRight  => Point(-rectangle.size.width, -rectangle.size.height)
    }
}

object AnchorPoint {
  case object TopLeft      extends AnchorPoint
  case object TopCenter    extends AnchorPoint
  case object TopRight     extends AnchorPoint
  case object CenterLeft   extends AnchorPoint
  case object Center       extends AnchorPoint
  case object CenterRight  extends AnchorPoint
  case object BottomLeft   extends AnchorPoint
  case object BottomCenter extends AnchorPoint
  case object BottomRight  extends AnchorPoint
}
