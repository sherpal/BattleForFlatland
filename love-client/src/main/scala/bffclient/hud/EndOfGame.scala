package bffclient.hud

import bffclient.game.GameClient
import bffclient.gui.*
import bffclient.hud.FightReport.Tab
import bffclient.ui.{ButtonStyle, Theme}

/** Shown over the game once it ended, or when the connection to the game server is lost: the
  * statistics of the fight (see [[FightReport]]), and back to the menus (when the game was joined
  * through them, also with Enter) or quit (also with Escape).
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

  // The statistics, once the fight started
  private val tabs = Segmented[Tab](box, _.label(client.i18n))
  tabs.setPoint(Point.TopLeft, box, Point.TopLeft, 24, 120)
  tabs.setPoint(Point.TopRight, box, Point.TopRight, -24, 120)
  tabs.setHeight(32)
  tabs.setScript(tabs.OnValueChanged)(show)
  tabs.hide()
  private val scroll = ScrollFrame(box)
  scroll.setPoint(Point.TopLeft, box, Point.TopLeft, 24, 166)
  scroll.setPoint(Point.BottomRight, box, Point.BottomRight, -24, -90)
  scroll.setFitsContents(true)
  scroll.hide()

  private var report   = Option.empty[FightReport]
  private var contents = Option.empty[Frame]

  private def show(tab: Tab): Unit = for r <- report do {
    contents.foreach(_.remove())
    val view = Frame(scroll.scrollChild)
    view.setPoint(Point.TopLeft, scroll.scrollChild, Point.TopLeft)
    val width = box.width - 48 - 16 // room for the scroll's thumb
    view.setWidth(width)
    view.setHeight(r.render(tab, view, width, () => show(tab)))
    contents = Some(view)
    scroll.setVerticalScroll(0)
  }

  /** Once the game is over, the statistics of the fight (if it started), in a larger box (as large
    * as the window allows).
    */
  private def showReport(): Unit = {
    val gameState = client.gameState
    if gameState.started then {
      val r = FightReport(client.fightLog, gameState, client.myId, client.i18n)
      report = Some(r)
      box.setSize(math.min(900.0, frame.width - 32), math.min(680.0, frame.height - 32))
      title.setPoint(Point.Center, box, Point.Top, 0, 42)
      subtitle.setPoint(Point.Center, box, Point.Top, 0, 86)
      tabs.show()
      scroll.show()
      tabs.setOptions(Tab.values.toSeq, Some(r.firstTab))
      show(r.firstTab)
    }
  }

  backToMenus match {
    case Some(back) =>
      val backButton = Button(box, client.i18n.backToMenus, ButtonStyle.Primary)
      backButton.setSize(250, 44)
      backButton.setPoint(Point.BottomLeft, 30, -26)
      backButton.setScript(Script.OnClick)(_ => back())
      val quitButton = Button(box, client.i18n.quit)
      quitButton.setSize(140, 44)
      quitButton.setPoint(Point.BottomRight, -30, -26)
      quitButton.setScript(Script.OnClick)(_ => client.quit())
    case None =>
      val quitButton = Button(box, client.i18n.quit, ButtonStyle.Primary)
      quitButton.setPoint(Point.BottomLeft, 30, -26)
      quitButton.setPoint(Point.BottomRight, -30, -26)
      quitButton.setHeight(44)
      quitButton.setScript(Script.OnClick)(_ => client.quit())
  }

  /** Shows it once the game is over (every frame). */
  def update(): Unit = {
    frame.setShown(client.finished)
    if frame.isShown then {
      if report.isEmpty then showReport()
      val gameState = client.gameState
      val (text, colour) =
        if !gameState.ended then (client.i18n.connectionLost, Theme.negative.lighter(0.3))
        else if gameState.playersWon then (client.i18n.victory, Theme.positive.lighter(0.3))
        else (client.i18n.defeat, Theme.negative.lighter(0.3))
      title.setText(text)
      title.setColour(colour)
      subtitle.setText((gameState.startTime, gameState.endTime) match {
        case (Some(start), Some(end)) =>
          val seconds = ((end - start) / 1000).toInt
          client.i18n.fightLasted(seconds / 60, seconds % 60)
        case _ if !gameState.ended => client.i18n.gameServerClosedConnection
        case _ => ""
      })
    }
  }
}
