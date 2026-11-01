package bffclient.gui

import bffclient.love.Timer
import bffclient.ui.Theme

/** Frames made in the theme's style (WoW's templates). */
object Templates {

  /** A frame with the panels' background and border. */
  def panel(parent: Frame): Frame = {
    val frame = Frame(parent)
    frame.setBackdrop(Backdrop.panel)
    frame
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
