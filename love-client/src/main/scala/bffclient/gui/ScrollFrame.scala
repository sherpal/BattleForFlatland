package bffclient.gui

import bffclient.ui.Theme

/** Shows a part of its [[scrollChild]], which is as wide as it (but taller): the contents go in the
  * scroll child, which must be given a height, or fit its contents (see [[setFitsContents]]). The
  * mouse wheel scrolls it, and a thumb on the right shows where it is.
  */
class ScrollFrame(parentFrame: Frame) extends Frame(parentFrame) {
  setClipsChildren(true)
  enableMouseWheel(true)

  val scrollChild: Frame = Frame(this)

  // over the contents
  private val bar        = Frame(this)
  private val thumb      = bar.createTexture(Theme.border, Layer.Overlay)
  private var scroll     = 0.0
  private var scrollStep = 44.0
  private var placedAt   = -1.0 // the scroll of the scroll child's anchors
  private var fits       = false

  bar.setAllPoints()
  thumb.setWidth(4)
  placeChild()

  def verticalScroll: Double = scroll

  /** How far it can scroll. */
  def verticalScrollRange: Double = math.max(0.0, scrollChild.height - height)

  def setVerticalScroll(offset: Double): Unit = {
    scroll = math.max(0.0, math.min(offset, verticalScrollRange))
    placeChild()
  }

  /** The scroll child is then as high as its contents (see [[Frame.contentBottom]]), plus 8. */
  def setFitsContents(fitsContents: Boolean): Unit = fits = fitsContents

  /** How much a turn of the wheel scrolls. */
  def setScrollStep(step: Double): Unit = scrollStep = step

  private def placeChild(): Unit = if placedAt != scroll then {
    placedAt = scroll
    scrollChild.setPoint(Point.TopLeft, this, Point.TopLeft, 0, -scroll)
    scrollChild.setPoint(Point.TopRight, this, Point.TopRight, 0, -scroll)
  }

  override protected[gui] def mouseWheel(delta: Double): Unit = {
    setVerticalScroll(scroll - delta * scrollStep)
    super.mouseWheel(delta)
  }

  override protected[gui] def refresh(gui: Gui): Unit = {
    if fits then {
      val contentHeight = scrollChild.contentBottom - scrollChild.top + 8
      if math.abs(contentHeight - scrollChild.height) > 0.5 then
        scrollChild.setHeight(contentHeight)
    }
    setVerticalScroll(scroll) // the contents may have changed
    bar.setFrameLevel(frameLevel + 20) // above the contents' frames
    val range = verticalScrollRange
    thumb.setShown(range > 0)
    if range > 0 then {
      val thumbHeight = math.max(20.0, height * height / scrollChild.height)
      val offset      = (height - thumbHeight) * scroll / range
      if thumb.height != thumbHeight then thumb.setHeight(thumbHeight)
      if thumb.points.headOption.forall(_.y != offset) then
        thumb.setPoint(Point.TopRight, bar, Point.TopRight, -3, offset)
    }
  }
}
