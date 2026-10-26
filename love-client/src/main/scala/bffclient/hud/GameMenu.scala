package bffclient.hud

import bffclient.game.GameClient
import bffclient.love.{Fullscreen, Graphics}
import bffclient.ui.*

/** The game menu, opened with Escape during the game. The game cannot be paused (it is played
  * online), so it goes on meanwhile. Surrendering (confirmed with a second click) ends the fight, in
  * a defeat, for everyone.
  */
final class GameMenu(client: GameClient, leaveGame: Option[() => Unit]) {
  private var confirmingSurrender = false

  def draw(ui: Ui): Unit = {
    val screen          = Graphics.getDimensions()
    val (width, height) = (screen.width.toDouble, screen.height.toDouble)
    ui.fill(Rect(0, 0, width, height), Theme.background.withAlpha(0.5))
    val canSurrender = client.canSurrender
    val buttonCount  = 3 + Seq(canSurrender, leaveGame.isDefined).count(identity)
    val box = Rect(width / 2 - 180, height / 2 - 70 - 30 * buttonCount, 360, 140 + 60 * buttonCount)
    ui.panel(box)
    ui.textIn("Menu", Rect(box.x, box.y + 16, box.width, 50), Theme.title)
    ui.textIn(
      if confirmingSurrender then "The fight ends in a defeat, for everyone" else "The game goes on meanwhile",
      Rect(box.x, box.y + 66, box.width, 24),
      Theme.small,
      if confirmingSurrender then Theme.negative.lighter(0.3) else Theme.muted
    )

    var index = 0
    def button(label: String, style: ButtonStyle) = {
      val clicked = ui.button(
        label,
        Rect(box.x + 40, box.y + 110 + 60 * index, box.width - 80, 44),
        style = style
      )
      index += 1
      clicked
    }

    if button("Resume", ButtonStyle.Primary) || ui.wasPressed("escape") then {
      client.menuOpen = false
      confirmingSurrender = false
    }
    if canSurrender then {
      val label = if confirmingSurrender then "Click again to surrender" else "Surrender"
      if button(label, if confirmingSurrender then ButtonStyle.Danger else ButtonStyle.Normal) then {
        if confirmingSurrender then client.surrender()
        confirmingSurrender = !confirmingSurrender
      }
    }
    if button(if Fullscreen.isOn then "Windowed (F11)" else "Fullscreen (F11)", ButtonStyle.Normal) then
      Fullscreen.toggle()
    leaveGame.foreach(leave => if button("Leave game", ButtonStyle.Normal) then leave())
    if button("Quit", ButtonStyle.Danger) then client.quit()
  }
}
