package bffclient.hud

import bffclient.game.GameClient
import bffclient.gui.*
import bffclient.love.Fullscreen
import bffclient.ui.{ButtonStyle, Theme}

/** The game menu, opened with Escape during the game (a dialog over the game, while
  * `client.menuOpen`). The game cannot be paused (it is played online), so it goes on meanwhile.
  * Surrendering (confirmed with a second click) ends the fight, in a defeat, for everyone.
  */
final class GameMenu(parent: Frame, client: GameClient, leaveGame: Option[() => Unit]) {
  private val i18n                = client.i18n
  private var confirmingSurrender = false

  val frame: Frame = Frame(parent)
  frame.setAllPoints()
  frame.setFrameStrata(Strata.Dialog)
  frame.setBackdrop(Backdrop(Theme.background.withAlpha(0.5)))
  frame.enableMouse(true)
  frame.enableKeyboard(true)
  frame.setScript(Script.OnKeyDown)(scancode => if scancode == "escape" then resume())
  frame.hide()

  private val box = Templates.panel(frame)
  box.setWidth(360)
  box.setPoint(Point.Center)
  private val title = box.createFontString(i18n.menu, Theme.title)
  title.setPoint(Point.Center, box, Point.Top, 0, 41)
  private val subtitle = box.createFontString(size = Theme.small)
  subtitle.setPoint(Point.Center, box, Point.Top, 0, 78)

  private def button(label: String, style: ButtonStyle)(onClick: => Unit): Button = {
    val button = Button(box, label, style)
    button.setScript(Script.OnClick)(_ => onClick)
    button
  }
  private val resumeButton = button(i18n.resume, ButtonStyle.Primary)(resume())
  private val surrenderButton = button(i18n.surrender, ButtonStyle.Normal) {
    if confirmingSurrender then client.surrender()
    confirmingSurrender = !confirmingSurrender
  }
  private val fullscreenButton = button("", ButtonStyle.Normal)(Fullscreen.toggle())
  private val leaveButton =
    leaveGame.map(leave => button(i18n.leaveGame, ButtonStyle.Normal)(leave()))
  private val quitButton = button(i18n.quit, ButtonStyle.Danger)(client.quit())

  /** The buttons shown, one under the other. */
  private var stacked = Seq.empty[Button]

  private def resume(): Unit = {
    client.menuOpen = false
    confirmingSurrender = false
  }

  /** Shows it while the menu is open (every frame). */
  def update(): Unit = {
    frame.setShown(client.menuOpen && !client.finished)
    if frame.isShown then {
      val canSurrender = client.canSurrender
      if !canSurrender then confirmingSurrender = false
      subtitle.setText(
        if confirmingSurrender then i18n.surrenderEndsInDefeat else i18n.gameGoesOnMeanwhile
      )
      subtitle.setColour(if confirmingSurrender then Theme.negative.lighter(0.3) else Theme.muted)
      surrenderButton.setText(
        if confirmingSurrender then i18n.clickAgainToSurrender else i18n.surrender
      )
      surrenderButton.setStyle(
        if confirmingSurrender then ButtonStyle.Danger else ButtonStyle.Normal
      )
      fullscreenButton.setText(if Fullscreen.isOn then i18n.windowed else i18n.fullscreen)

      val buttons = Seq(resumeButton) ++ Option.when(canSurrender)(surrenderButton) ++
        Seq(fullscreenButton) ++ leaveButton ++ Seq(quitButton)
      if buttons != stacked then {
        stacked = buttons
        for b <- Seq(resumeButton, surrenderButton, fullscreenButton, quitButton) ++ leaveButton do
          b.setShown(buttons.contains(b))
        for (b, i) <- buttons.zipWithIndex do {
          b.setPoint(Point.TopLeft, box, Point.TopLeft, 40, 110 + 60 * i)
          b.setPoint(Point.TopRight, box, Point.TopRight, -40, 110 + 60 * i)
          b.setHeight(44)
        }
        box.setHeight(140 + 60 * buttons.size)
      }
    } else confirmingSurrender = false
  }
}
