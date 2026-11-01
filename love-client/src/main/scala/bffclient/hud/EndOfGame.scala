package bffclient.hud

import bffclient.game.GameClient
import bffclient.gui.*
import bffclient.ui.{ButtonStyle, Theme}

/** Shown over the game once it ended, or when the connection to the game server is lost: back to
  * the menus (when the game was joined through them, also with Enter) or quit (also with Escape).
  */
final class EndOfGame(parent: Frame, client: GameClient, backToMenus: Option[() => Unit]) {
  val frame: Frame = Frame(parent)
  frame.setAllPoints()
  frame.setFrameStrata(Strata.Dialog)
  frame.setBackdrop(Backdrop(Theme.background.withAlpha(0.5)))
  frame.enableMouse(true)
  frame.enableKeyboard(true)
  frame.setScript(Script.OnKeyDown) {
    case "return" | "kpenter" => backToMenus.fold(client.quit())(_())
    case "escape" => client.quit()
    case _ => ()
  }
  frame.hide()

  private val box = Templates.panel(frame)
  box.setSize(460, 220)
  box.setPoint(Point.Center)
  private val title = box.createFontString(size = Theme.title)
  title.setPoint(Point.Center, box, Point.Top, 0, 45)
  private val subtitle = box.createFontString(colour = Theme.muted)
  subtitle.setPoint(Point.Center, box, Point.Top, 0, 93)

  backToMenus match {
    case Some(back) =>
      val backButton = Button(box, "Back to menus", ButtonStyle.Primary)
      backButton.setSize(250, 44)
      backButton.setPoint(Point.BottomLeft, 30, -26)
      backButton.setScript(Script.OnClick)(_ => back())
      val quitButton = Button(box, "Quit")
      quitButton.setSize(140, 44)
      quitButton.setPoint(Point.BottomRight, -30, -26)
      quitButton.setScript(Script.OnClick)(_ => client.quit())
    case None =>
      val quitButton = Button(box, "Quit", ButtonStyle.Primary)
      quitButton.setPoint(Point.BottomLeft, 30, -26)
      quitButton.setPoint(Point.BottomRight, -30, -26)
      quitButton.setHeight(44)
      quitButton.setScript(Script.OnClick)(_ => client.quit())
  }

  /** Shows it once the game is over (every frame). */
  def update(): Unit = {
    frame.setShown(client.finished)
    if frame.isShown then {
      val gameState = client.gameState
      val (text, colour) =
        if !gameState.ended then ("Connection lost", Theme.negative.lighter(0.3))
        else if gameState.playersWon then ("Victory!", Theme.positive.lighter(0.3))
        else ("Defeat...", Theme.negative.lighter(0.3))
      title.setText(text)
      title.setColour(colour)
      subtitle.setText((gameState.startTime, gameState.endTime) match {
        case (Some(start), Some(end)) =>
          val seconds = ((end - start) / 1000).toInt
          f"The fight lasted ${seconds / 60}%d:${seconds % 60}%02d"
        case _ if !gameState.ended => "The game server closed the connection"
        case _ => ""
      })
    }
  }
}
