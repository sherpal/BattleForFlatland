package game.ui

import indigo.*

final case class Anchor(point: AnchorPoint, relativePoint: AnchorPoint, offset: Point) {

  def withOffset(newOffset: Point): Anchor =
    copy(offset = newOffset)

}

object Anchor {
  private def simpleAnchor(anchorPoint: AnchorPoint) = Anchor(anchorPoint, anchorPoint, Point.zero)

  val top: Anchor         = simpleAnchor(AnchorPoint.TopCenter)
  val topLeft: Anchor     = simpleAnchor(AnchorPoint.TopLeft)
  val topRight: Anchor    = simpleAnchor(AnchorPoint.TopRight)
  val bottomLeft: Anchor  = simpleAnchor(AnchorPoint.BottomLeft)
  val bottomRight: Anchor = simpleAnchor(AnchorPoint.BottomRight)
  val center: Anchor      = simpleAnchor(AnchorPoint.Center)
  val bottom: Anchor      = simpleAnchor(AnchorPoint.BottomCenter)
  val right: Anchor       = simpleAnchor(AnchorPoint.CenterRight)
  val left: Anchor        = simpleAnchor(AnchorPoint.CenterLeft)
}
