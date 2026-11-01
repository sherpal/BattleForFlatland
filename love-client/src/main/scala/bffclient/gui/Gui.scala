package bffclient.gui

import scala.collection.mutable

import bffclient.love.{Graphics, Mouse, Timer}
import bffclient.ui.Rect

/** The frames of a UI (WoW's UI engine): it draws them every frame, and gives them LÖVE's inputs.
  *
  * Frames are created in [[root]] (or in its descendants), and stay there until they are removed:
  * the UI is retained. LÖVE's callbacks are forwarded to [[mousePressed]], [[mouseReleased]],
  * [[wheelMoved]], [[keyPressed]] and [[textInput]], which say whether a frame took the input;
  * [[draw]] runs the `OnUpdate` scripts and draws the visible frames. The mouse's positions are in
  * the window's pixels, converted for each frame with its scale ([[Frame.setScale]]).
  */
final class Gui {
  val root: UIParent = UIParent(this)

  // Per frame, without allocations (the drawing is hot): the visible frames, in the order they were
  // last drawn, and the ones of the `OnUpdate` scripts
  private val ordered      = mutable.ArrayBuffer.empty[Frame]
  private val updated      = mutable.ArrayBuffer.empty[Frame]
  private var sortingA     = new Array[Frame](0)
  private var sortingB     = new Array[Frame](0)
  private var hoveredFrame = Option.empty[Frame]
  private var pressedFrame = Option.empty[(Frame, Int)]
  private var focusedBox   = Option.empty[EditBox]
  private var lastTime     = -1.0
  private var windowWidth  = -1
  private var windowHeight = -1

  def isHovered(frame: Frame): Boolean = hoveredFrame match {
    case Some(hovered) => hovered eq frame
    case None => false
  }

  /** The topmost frame under the mouse with the mouse enabled, as of the last frame. */
  def hovered: Option[Frame] = hoveredFrame

  /** Whether the mouse was pressed on the frame (and not released yet). */
  def isPressed(frame: Frame): Boolean = pressedFrame.exists(_._1 eq frame)

  /** The edit box that gets the keyboard, if any. */
  def focus: Option[EditBox]           = focusedBox
  def setFocus(editBox: EditBox): Unit = focusedBox = Some(editBox)
  def clearFocus(): Unit               = focusedBox = None

  // --- Drawing ---

  def draw(): Unit = {
    val now     = Timer.getTime()
    val elapsed = if lastTime < 0 then 0.0 else now - lastTime
    lastTime = now
    val window = Graphics.getDimensions()
    if window.width != windowWidth || window.height != windowHeight then {
      windowWidth = window.width
      windowHeight = window.height
      Region.invalidate()
    }

    updated.clear()
    collectShown(root)
    var i = 0
    while i < updated.length do {
      val frame = updated(i)
      // (an earlier script may have hidden it)
      if frame.hasScripts && frame.isVisible then frame.fire(Script.OnUpdate)(_(elapsed))
      i += 1
    }
    focusedBox match {
      case Some(box) if !box.isVisible => clearFocus()
      case _ => ()
    }

    ordered.clear()
    prepare(root, Strata.Medium.ordinal, -1, 1.0, 1.0, None)
    sortOrdered()
    val mouse = Mouse.getPosition()
    updateHovered(mouse.x, mouse.y)
    i = 0
    while i < ordered.length do { ordered(i).refresh(this); i += 1 }

    val previousFont = Graphics.getFont()
    // changing the scissor ends LÖVE's batch of drawings: only when it changes
    var scissor = Option.empty[Rect]
    Graphics.setScissor()
    i = 0
    while i < ordered.length do {
      val frame = ordered(i)
      val clip  = frame.drawClip
      if clip != scissor then {
        scissor = clip
        clip match {
          case Some(clip) => Graphics.setScissor(clip.x, clip.y, clip.width, clip.height)
          case None => Graphics.setScissor()
        }
      }
      val scale = frame.drawScale
      if scale != 1 then { Graphics.push(); Graphics.scale(scale) }
      frame.draw()
      if scale != 1 then Graphics.pop()
      i += 1
    }
    Graphics.setScissor()
    Graphics.setFont(previousFont)
  }

  /** The shown frames whose ancestors are shown, parents before their children. */
  private def collectShown(frame: Frame): Unit = if frame.isShown then {
    updated += frame
    val children = frame.children
    var i        = 0
    while i < children.length do { collectShown(children(i)); i += 1 }
  }

  /** Puts the visible frames in [[ordered]], in the tree's order, with what drawing them needs
    * (computed once from their parent's rather than for each of them from the root).
    */
  private def prepare(
      frame: Frame,
      parentStrata: Int,
      parentLevel: Int,
      parentAlpha: Double,
      parentScale: Double,
      parentClip: Option[Rect]
  ): Unit = if frame.isShown then {
    val strata = frame.strataOrdinal(parentStrata)
    val level  = frame.levelFrom(parentLevel)
    val alpha  = parentAlpha * frame.getAlpha
    val scale  = parentScale * frame.getScale
    frame.drawAlpha = alpha
    frame.drawScale = scale
    val clip = frame.clipFrom(parentClip)
    frame.drawClip = clip
    // by stratum, then level, then in the tree's order (levels are within +-1e6, and there are less
    // than 1e6 frames)
    frame.drawKey = strata * 1e13 + (level + 1e6) * 1e6 + ordered.length
    ordered += frame
    val children = frame.children
    var i        = 0
    while i < children.length do {
      prepare(children(i), strata, level, alpha, scale, clip)
      i += 1
    }
  }

  /** By `drawKey`: a merge sort, which only compares once the frames are already in order (most of
    * them are: children are after their parent, with a higher level).
    */
  private def sortOrdered(): Unit = {
    val n       = ordered.length
    var inOrder = true
    var i       = 1
    while inOrder && i < n do { inOrder = ordered(i - 1).drawKey <= ordered(i).drawKey; i += 1 }
    if !inOrder then {
      if sortingA.length < n then {
        sortingA = new Array[Frame](n * 2); sortingB = new Array[Frame](n * 2)
      }
      var from = sortingA
      i = 0
      while i < n do { from(i) = ordered(i); i += 1 }
      var to    = sortingB
      var width = 1
      while width < n do {
        var start = 0
        while start < n do {
          val middle = math.min(start + width, n)
          val end    = math.min(start + 2 * width, n)
          var l      = start
          var r      = middle
          var k      = start
          while k < end do {
            if l < middle && (r >= end || from(l).drawKey <= from(r).drawKey) then {
              to(k) = from(l); l += 1
            } else { to(k) = from(r); r += 1 }
            k += 1
          }
          start = end
        }
        val swap = from; from = to; to = swap
        width *= 2
      }
      i = 0
      while i < n do { ordered(i) = from(i); from(i) = null; to(i) = null; i += 1 }
    }
  }

  /** The topmost frame at (x, y), in the window's pixels, accepted by `accepts`. */
  private def frameAt(x: Double, y: Double, accepts: Frame => Boolean): Option[Frame] = {
    var i     = ordered.length - 1
    var found = Option.empty[Frame]
    while found.isEmpty && i >= 0 do {
      val frame = ordered(i)
      if accepts(frame) && frame.isVisible && frame.containsWindowPoint(x, y) then
        found = Some(frame)
      i -= 1
    }
    found
  }

  private def updateHovered(x: Double, y: Double): Unit =
    setHovered(frameAt(x, y, _.isMouseEnabled))

  private def setHovered(frame: Option[Frame]): Unit =
    if frame != hoveredFrame then {
      hoveredFrame.foreach(_.fire(Script.OnLeave)(_()))
      hoveredFrame = frame
      frame.foreach(_.fire(Script.OnEnter)(_()))
    }

  // --- Inputs (each says whether a frame took it), with the mouse in the window's pixels ---

  def mousePressed(x: Double, y: Double, button: Int): Boolean = {
    updateHovered(x, y)
    val target = frameAt(x, y, _.takesButton(button))
    if focusedBox.exists(box => !target.exists(_ eq box)) then clearFocus()
    target.foreach { frame =>
      pressedFrame = Some((frame, button))
      frame.mouseDown(button)
    }
    target.isDefined
  }

  def mouseReleased(x: Double, y: Double, button: Int): Boolean = pressedFrame match {
    case Some((frame, pressedButton)) if pressedButton == button =>
      pressedFrame = None
      frame.mouseUp(button)
      if frame.isVisible && frameAt(x, y, _.takesButton(button)).exists(_ eq frame) then
        frame.click(button)
      true
    case _ => false
  }

  /** `dy` positive upwards. Goes to the topmost frame under the mouse with the wheel enabled. */
  def wheelMoved(dy: Double): Boolean = {
    val mouse  = Mouse.getPosition()
    val target = frameAt(mouse.x, mouse.y, _.isMouseWheelEnabled)
    target.foreach(_.mouseWheel(dy))
    target.isDefined
  }

  /** Goes to the focused edit box, otherwise to the topmost frames with the keyboard enabled. */
  def keyPressed(scancode: String): Boolean =
    focusedBox.filter(_.isVisible) match {
      case Some(box) =>
        box.keyDown(scancode)
        true
      case None =>
        var i        = ordered.length - 1
        var consumed = false
        while !consumed && i >= 0 do {
          val frame = ordered(i)
          if frame.isKeyboardEnabled && frame.isVisible then {
            frame.keyDown(scancode)
            consumed = !frame.propagatesKeyboardInput
          }
          i -= 1
        }
        consumed
    }

  /** Text typed (UTF-8), for the focused edit box. */
  def textInput(text: String): Boolean = {
    val box = focusedBox.filter(_.isVisible)
    box.foreach(_.insert(text))
    box.isDefined
  }
}
