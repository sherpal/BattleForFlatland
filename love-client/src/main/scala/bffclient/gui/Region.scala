package bffclient.gui

import scala.collection.mutable

import bffclient.ui.Rect

/** Something placed on the screen: a [[Frame]], or what frames draw ([[Texture]], [[FontString]]).
  *
  * Like in WoW, a region is placed with anchors: `setPoint(Point.TopLeft, other, Point.BottomLeft,
  * 0, 8)` puts its top left corner 8 pixels below the bottom left corner of `other`. Each axis is
  * solved on its own: two anchors with different x fractions (e.g. left and right) give the left
  * edge and the width, otherwise one anchor and the width ([[setWidth]], or the size of a text) do.
  * Same for y. A region without anchors is not drawn.
  *
  * Positions, sizes and offsets are in the units of the region's frame, which are its scale's
  * ([[Frame.setScale]]): a region anchored to one of another scale is placed where it is on the
  * screen.
  *
  * Positions are computed when asked for, and cached: a change (of an anchor, a size, a text) only
  * computes again the region and the ones anchored to it (and to those, ...), while a change of
  * everything (the window's size, a scale, a parent) calls [[Region.invalidate]].
  */
// (an abstract class rather than a trait: its fields are then plain fields in Lua, not accessors)
abstract class Region {
  def parent: Option[Frame]

  /** How big one of its units is, in the window's pixels (its frame's, and its ancestors' scales).
    */
  def effectiveScale: Double

  /** The frame whose [[effectiveScale]] its own multiplies, and by how much (its frame and 1 for a
    * texture or a text, its parent and its scale for a frame): anchors between regions in the same
    * frame are converted with them, without asking the effective scales.
    */
  private[gui] def scaleOwner: Frame | Null
  private[gui] def localScale: Double

  private val anchors        = mutable.ArrayBuffer.empty[Anchor]
  private var explicitWidth  = Option.empty[Double]
  private var explicitHeight = Option.empty[Double]
  private var shown          = true
  // the cached positions, without tuples (they are asked for by every region, every frame)
  private var horizontalStart      = 0.0
  private var horizontalSize       = 0.0
  private var verticalStart        = 0.0
  private var verticalSize         = 0.0
  private var horizontalGeneration = -1
  private var verticalGeneration   = -1
  private var resolvingHorizontal  = false
  private var resolvingVertical    = false
  // whether it changed since it was solved, and the regions anchored to it
  private var horizontalDirty = true
  private var verticalDirty   = true
  private val dependents      = mutable.ArrayBuffer.empty[Region]

  /** Its position must be computed again, and so must the ones of the regions anchored to it (each
    * one once, even if it is anchored to several of them).
    */
  protected[gui] def layoutChanged(): Unit = {
    Region.changes += 1
    markChanged(Region.changes)
  }

  private var lastChange = -1

  private def markChanged(change: Int): Unit = if lastChange != change then {
    lastChange = change
    horizontalDirty = true
    verticalDirty = true
    var i = 0
    while i < dependents.length do { dependents(i).markChanged(change); i += 1 }
  }

  /** Forgets that it depends on the anchor's region, unless another anchor does too. */
  private def unlink(anchor: Anchor): Unit = {
    val relativeTo = anchor.relativeTo
    if !anchors.exists(other => (other ne anchor) && (other.relativeTo eq relativeTo)) then
      relativeTo.dependents.filterInPlace(_ ne this)
  }

  // --- Anchors and size ---

  /** Replaces the anchor of the same `point`, if any. */
  def setPoint(
      point: Point,
      relativeTo: Region,
      relativePoint: Point,
      x: Double = 0,
      y: Double = 0
  ): Unit = {
    require(relativeTo ne this, "A region cannot be anchored to itself")
    val anchor = Anchor(point, relativeTo, relativePoint, x, y)
    val index  = anchors.indexWhere(_.point == point)
    if index < 0 || anchors(index) != anchor then {
      if index >= 0 then {
        val previous = anchors(index)
        anchors.remove(index)
        unlink(previous)
      }
      anchors += anchor
      if !relativeTo.dependents.exists(_ eq this) then relativeTo.dependents += this
      layoutChanged()
    }
  }

  /** Anchored to the same point of the parent. */
  def setPoint(point: Point, x: Double, y: Double): Unit =
    setPoint(point, parentOrFail, point, x, y)

  def setPoint(point: Point): Unit = setPoint(point, parentOrFail, point)

  /** Covers `region` exactly. */
  def setAllPoints(region: Region): Unit = {
    clearAllPoints()
    setPoint(Point.TopLeft, region, Point.TopLeft)
    setPoint(Point.BottomRight, region, Point.BottomRight)
  }

  def setAllPoints(): Unit = setAllPoints(parentOrFail)

  def clearAllPoints(): Unit = if anchors.nonEmpty then {
    for anchor <- anchors do anchor.relativeTo.dependents.filterInPlace(_ ne this)
    anchors.clear()
    layoutChanged()
  }

  def points: Seq[Anchor] = anchors.toSeq

  def setWidth(width: Double): Unit = if !explicitWidth.contains(width) then {
    explicitWidth = Some(width)
    layoutChanged()
  }
  def setHeight(height: Double): Unit = if !explicitHeight.contains(height) then {
    explicitHeight = Some(height)
    layoutChanged()
  }
  def setSize(width: Double, height: Double): Unit = { setWidth(width); setHeight(height) }

  /** The size when the anchors do not give it, e.g. the one of a text. */
  protected def intrinsicWidth: Double  = 0
  protected def intrinsicHeight: Double = 0

  // --- Position on the screen ---

  def left: Double   = { solveHorizontal(); horizontalStart }
  def width: Double  = { solveHorizontal(); horizontalSize }
  def right: Double  = left + width
  def top: Double    = { solveVertical(); verticalStart }
  def height: Double = { solveVertical(); verticalSize }
  def bottom: Double = top + height
  def rect: Rect = {
    solveHorizontal(); solveVertical()
    Rect(horizontalStart, verticalStart, horizontalSize, verticalSize)
  }

  /** Whether it was given a place on the screen. */
  def isAnchored: Boolean = anchors.nonEmpty

  /** With (x, y) in its units. */
  def isMouseOver(x: Double, y: Double): Boolean = isAnchored && rect.contains(x, y)

  private def solveHorizontal(): Unit =
    if horizontalDirty || horizontalGeneration != Region.generation then {
      if resolvingHorizontal then throw IllegalStateException(s"Anchor cycle through $this")
      resolvingHorizontal = true
      solve(isHorizontal = true)
      resolvingHorizontal = false
      horizontalDirty = false
      horizontalGeneration = Region.generation
    }

  private def solveVertical(): Unit =
    if verticalDirty || verticalGeneration != Region.generation then {
      if resolvingVertical then throw IllegalStateException(s"Anchor cycle through $this")
      resolvingVertical = true
      solve(isHorizontal = false)
      resolvingVertical = false
      verticalDirty = false
      verticalGeneration = Region.generation
    }

  /** Where it is on an axis (left and width, or top and height): [[solve]]'s result. */
  protected final def solved(isHorizontal: Boolean, start: Double, size: Double): Unit =
    if isHorizontal then { horizontalStart = start; horizontalSize = size }
    else { verticalStart = start; verticalSize = size }

  /** The start and size on an axis (given to [[solved]]): each anchor says that `start + fraction *
    * size` is somewhere.
    */
  protected def solve(isHorizontal: Boolean): Unit = {
    var fraction1 = -1.0
    var at1       = 0.0
    var fraction2 = -1.0
    var at2       = 0.0
    var i         = 0
    // (a loop rather than a closure: it runs for every region, every frame)
    while i < anchors.length do {
      val anchor = anchors(i)
      i += 1
      val fraction = if isHorizontal then anchor.point.x else anchor.point.y
      if fraction2 < 0 && fraction != fraction1 then {
        val relativeTo = anchor.relativeTo
        val start      = if isHorizontal then relativeTo.left else relativeTo.top
        val size       = if isHorizontal then relativeTo.width else relativeTo.height
        // in our units
        val factor =
          if relativeTo eq scaleOwner then 1.0 / localScale
          else if relativeTo.scaleOwner eq scaleOwner then relativeTo.localScale / localScale
          else relativeTo.effectiveScale / effectiveScale
        val at =
          if isHorizontal then (start + anchor.relativePoint.x * size) * factor + anchor.x
          else (start + anchor.relativePoint.y * size) * factor + anchor.y
        if fraction1 < 0 then { fraction1 = fraction; at1 = at }
        else { fraction2 = fraction; at2 = at }
      }
    }
    if fraction2 >= 0 then {
      val size = math.max(0.0, (at2 - at1) / (fraction2 - fraction1))
      solved(isHorizontal, at1 - fraction1 * size, size)
    } else {
      val size = (if isHorizontal then explicitWidth else explicitHeight) match {
        case Some(size) => size
        case None => if isHorizontal then intrinsicWidth else intrinsicHeight
      }
      solved(isHorizontal, if fraction1 >= 0 then at1 - fraction1 * size else 0.0, size)
    }
  }

  // --- Visibility ---

  /** Whether it is not hidden itself (its parent may be). */
  def isShown: Boolean = shown

  /** Whether it and all its ancestors are shown. */
  def isVisible: Boolean = shown && (parent match {
    case Some(frame) => frame.isVisible
    case None => false
  })

  def show(): Unit                     = shown = true
  def hide(): Unit                     = shown = false
  def setShown(visible: Boolean): Unit = if visible then show() else hide()

  protected def parentOrFail: Frame =
    parent.getOrElse(throw IllegalStateException(s"$this has no parent"))
}

object Region {

  /** All the positions are computed again once this changes. */
  private[gui] var generation = 0

  /** Counts the changes, to mark each region once per change. */
  private[gui] var changes = 0

  /** To call when something changes all the positions (the screen's size, a scale, a parent). */
  def invalidate(): Unit = generation += 1
}
