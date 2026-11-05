package bffclient.gui

import bffclient.love.Timer
import bffclient.ui.{Colour, Theme}

/** Frames made in the theme's style (WoW's templates). */
object Templates {

  /** A frame with the panels' background and border. */
  def panel(parent: Frame): Frame = {
    val frame = Frame(parent)
    frame.setBackdrop(Backdrop.panel)
    frame
  }

  /** Draws the eye to a frame (of its final size), e.g. a button never used: a slanted gleam of
    * light crosses it from left to right every few seconds, like a reflection on metal, and a glow
    * in `colour` pulses around it.
    */
  def gleam(frame: Frame, colour: Colour): Unit = {
    val period = 3.0 // seconds, of which the gleam crosses during `sweep`
    val sweep  = 0.8
    val rows   = 16
    val slant  = 1.2 // pixels to the left per row, from the top
    // brighter in its middle: (from its left, width, opacity)
    val bands =
      Seq((0.0, 4.0, 0.1), (4.0, 4.0, 0.25), (8.0, 6.0, 0.5), (14.0, 4.0, 0.25), (18.0, 4.0, 0.1))
    val bandWidth = 22.0 + rows * slant

    val light = Frame(frame)
    light.setAllPoints()
    light.setClipsChildren(true) // the gleam stays on the frame
    // the band's rows are anchored to its head, the only region moved every frame
    val head = light.createTexture(Colour(0, 0, 0, 0), Layer.Overlay)
    head.setSize(1, 1)
    val rowHeight = frame.height / rows
    for
      row                    <- 0 until rows
      (left, width, opacity) <- bands
    do {
      val part = light.createTexture(Colour(1, 1, 1, opacity), Layer.Overlay)
      part.setSize(width, rowHeight)
      part.setPoint(
        Point.TopLeft,
        head,
        Point.TopLeft,
        left + (rows - 1 - row) * slant,
        row * rowHeight
      )
    }

    val glow = frame.createTexture(Colour(0, 0, 0, 0), Layer.Border)
    glow.setPoint(Point.TopLeft, frame, Point.TopLeft, -3, -3)
    glow.setPoint(Point.BottomRight, frame, Point.BottomRight, 3, 3)
    glow.setCornerRadius(4)

    // the first gleam soon after the frame appears
    val start = Timer.getTime() - period + 0.6
    light.setScript(Script.OnUpdate) { _ =>
      val time     = Timer.getTime() - start
      val progress = (time % period) / sweep // from 0 to 1 while it crosses
      val x =
        if progress < 1 then -bandWidth + progress * (frame.width + bandWidth) else -bandWidth
      head.setPoint(Point.TopLeft, light, Point.TopLeft, x, 0)
      val pulse = 0.5 + 0.5 * math.sin(time * 2 * math.Pi / period)
      glow.setOutline(Some(colour.lighter(0.3).withAlpha(0.25 + 0.45 * pulse)), 2)
    }
  }

  /** "Loading..." with moving dots, its top left corner where the frame is anchored. */
  def loading(parent: Frame, message: String): Frame = {
    val frame = Frame(parent)
    val text  = frame.createFontString(colour = Theme.muted)
    text.setPoint(Point.TopLeft, frame, Point.TopLeft)
    frame.setScript(Script.OnUpdate)(_ =>
      text.setText(message + "." * ((Timer.getTime() * 3).toInt % 4))
    )
    frame
  }
}
