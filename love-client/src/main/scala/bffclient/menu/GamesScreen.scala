package bffclient.menu

import scala.concurrent.ExecutionContext

import bffclient.ui.*
import menus.data.User
import models.bff.outofgame.MenuGameWithPlayers

/** The games that did not start yet, like the frontend's `GameTable`. */
final class GamesScreen(app: MenuApp, user: User)(using ExecutionContext) extends Screen {
  private var games       = Option.empty[Vector[MenuGameWithPlayers]]
  private val refresh     = Refresh(app, () => app.api.games)(received)
  private var lobby       = Option.empty[Lobby]
  private var busy        = false
  private val defaultName = s"${user.name}'s game"
  private val newGameName = TextInput(defaultName, maxLength = 32)

  private def received(all: Vector[MenuGameWithPlayers]): Unit = {
    games = Some(all.filterNot(_.started))
    // Names are unique, and started games (not shown) keep theirs
    if newGameName.text.startsWith(defaultName) then {
      val taken = all.map(_.game.gameName).toSet
      newGameName.text =
        (Iterator.single(defaultName) ++ Iterator.from(2).map(i => s"$defaultName $i"))
          .find(!taken(_))
          .get
    }
  }

  refresh()
  // Any change on the server is announced on the lobby websocket
  app.attempt(app.api.lobby(None)(_ => refresh()))(opened => lobby = Some(opened))

  override def leave(): Unit = lobby.foreach(_.close())

  def draw(ui: Ui): Unit = {
    val (width, height) = app.size
    ui.text("Battle for Flatland", 40, 30, Theme.title)
    ui.textIn(
      s"Welcome, ${user.name}",
      Rect(0, 40, width - 40, 30),
      Theme.body,
      Theme.muted,
      "right"
    )

    val gameYouAreIn = games.flatMap(_.find(_.containsPlayer(user)))
    gameYouAreIn match {
      case Some(game) =>
        val strip = Rect(40, 100, width - 230, 50) // the controls button is on its right
        ui.fill(strip, Theme.primary.withAlpha(0.25))
        ui.textIn(
          s"You are currently involved in the game '${game.game.gameName}'.",
          Rect(strip.x + 16, strip.y, strip.width, strip.height),
          align = "left"
        )
        if
          ui.button(
            "Join it back",
            Rect(strip.right - 150, strip.y + 7, 140, 36),
            !busy,
            ButtonStyle.Primary
          )
        then
          enter(game.id)
      case None if games.isDefined =>
        val entered =
          ui.textField(newGameName, Rect(40, 105, 320, 40), "Game name", enabled = !busy)
        if
          (ui.button(
            "Create game",
            Rect(370, 105, 150, 40),
            !busy,
            ButtonStyle.Primary
          ) || entered) && !busy
        then
          create()
      case None => ()
    }

    if ui.button("Controls", Rect(width - 170, 105, 130, 40)) then app.showControls()

    val table   = Rect(40, 180, width - 80, height - 220)
    val columns = Seq("Name" -> 0.0, "Created by" -> 0.4, "Players" -> 0.65)
    ui.panel(table)
    for (title, at) <- columns do
      ui.text(title, table.x + 16 + at * table.width, table.y + 12, colour = Theme.muted)
    games match {
      case None => ui.loading("Loading the games", table.x + 16, table.y + 50)
      case Some(Vector()) =>
        ui.text(
          "There are currently no games. Go ahead and create one!",
          table.x + 16,
          table.y + 50
        )
      case Some(all) =>
        val rowsThatFit = ((table.height - 50) / 44).toInt
        for (game, i) <- all.take(rowsThatFit).zipWithIndex do {
          val row = Rect(table.x + 1, table.y + 44 + 44 * i, table.width - 2, 44)
          if i % 2 == 0 then ui.fill(row, Theme.background.withAlpha(0.35))
          val cells =
            Seq(game.game.gameName, game.game.gameCreator.name, game.players.length.toString)
          for (cell, (_, at)) <- cells.zip(columns) do
            ui.textIn(
              cell,
              Rect(table.x + 16 + at * table.width, row.y, table.width * 0.25, row.height),
              align = "left"
            )
          // Like the frontend: one may join any game, unless already in one (which may then be joined back)
          val canJoin = gameYouAreIn.forall(_.id == game.id)
          if
            canJoin && ui.button(
              if gameYouAreIn.isDefined then "Join back" else "Join",
              Rect(row.right - 130, row.y + 5, 115, 34),
              !busy
            )
          then
            if gameYouAreIn.isDefined then enter(game.id) else join(game.id)
        }
        if all.size > rowsThatFit then
          ui.text(
            s"... and ${all.size - rowsThatFit} more",
            table.x + 16,
            table.bottom - 28,
            Theme.small,
            Theme.muted
          )
    }
  }

  private def create(): Unit = {
    busy = true
    app
      .attempt(app.api.createGame(newGameName.text.trim))(game => enter(game.id), _ => busy = false)
  }

  private def join(gameId: String): Unit = {
    busy = true
    app.attempt(app.api.joinGame(gameId))(_ => enter(gameId), _ => busy = false)
  }

  private def enter(gameId: String): Unit = app.show(new GameLobbyScreen(app, user, gameId))
}
