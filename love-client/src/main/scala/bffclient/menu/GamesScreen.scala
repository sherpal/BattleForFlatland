package bffclient.menu

import scala.collection.mutable
import scala.concurrent.ExecutionContext

import bffclient.gui.*
import bffclient.ui.{ButtonStyle, Theme}
import menus.data.User
import models.bff.outofgame.MenuGameWithPlayers

/** The games that did not start yet, like the frontend's `GameTable`. */
final class GamesScreen(app: MenuApp, user: User)(using ExecutionContext) extends Screen {
  private var games       = Option.empty[Vector[MenuGameWithPlayers]]
  private val refresh     = Refresh(app, () => app.api.games)(received)
  private var lobby       = Option.empty[Lobby]
  private var busy        = false
  private val defaultName = s"${user.name}'s game"

  private val root = Frame(app.gui.root)
  root.setAllPoints()

  root.createFontString("Battle for Flatland", Theme.title).setPoint(Point.TopLeft, 40, 30)
  root
    .createFontString(s"Welcome, ${user.name}", colour = Theme.muted)
    .setPoint(Point.Right, root, Point.TopRight, -40, 55)

  private val settingsButton = Button(root, "Settings")
  settingsButton.setSize(130, 40)
  settingsButton.setPoint(Point.TopRight, -40, 105)
  settingsButton.setScript(Script.OnClick)(_ => app.showSettings())

  // The game we are in, if any (the settings button is on its right)
  private val currentGame = Frame(root)
  currentGame.setBackdrop(Backdrop(Theme.primary.withAlpha(0.25)))
  currentGame.setHeight(50)
  currentGame.setPoint(Point.TopLeft, 40, 100)
  currentGame.setPoint(Point.TopRight, -190, 100)
  private val currentGameText = currentGame.createFontString()
  currentGameText.setPoint(Point.Left, 16, 0)
  private val joinBackButton = Button(currentGame, "Join it back", ButtonStyle.Primary)
  joinBackButton.setSize(140, 36)
  joinBackButton.setPoint(Point.TopRight, -10, 7)

  // Otherwise, a new game
  private val newGameName = EditBox(root, defaultName, "Game name", maxLetters = 32)
  newGameName.setSize(320, 40)
  newGameName.setPoint(Point.TopLeft, 40, 105)
  newGameName.setScript(Script.OnEnterPressed)(() => create())
  private val createButton = Button(root, "Create game", ButtonStyle.Primary)
  createButton.setSize(150, 40)
  createButton.setPoint(Point.TopLeft, newGameName, Point.TopRight, 10, 0)
  createButton.setScript(Script.OnClick)(_ => create())

  private val table = Templates.panel(root)
  table.setPoint(Point.TopLeft, 40, 180)
  table.setPoint(Point.BottomRight, -40, -40)
  private val columns = Seq(0.0, 0.4, 0.65) // where they start, in the table's width
  for (title, at) <- Seq("Name", "Created by", "Players").zip(columns) do
    table
      .createFontString(title, colour = Theme.muted)
      .setPoint(Point.TopLeft, table, Point(at, 0), 16, 12)

  private val loading = Templates.loading(table, "Loading the games")
  loading.setPoint(Point.TopLeft, 16, 50)
  private val noGames =
    table.createFontString("There are currently no games. Go ahead and create one!")
  noGames.setPoint(Point.TopLeft, 16, 50)

  private val list = ScrollFrame(table)
  list.setPoint(Point.TopLeft, 1, 44)
  list.setPoint(Point.BottomRight, -1, -1)

  /** The rows of the games, made when needed and reused. */
  private val rows = mutable.ArrayBuffer.empty[GameRow]

  private final class GameRow(index: Int) {
    var game = Option.empty[MenuGameWithPlayers]

    val frame = Frame(list.scrollChild)
    frame.setHeight(GamesScreen.rowHeight)
    frame.setPoint(Point.TopLeft, 0, index * GamesScreen.rowHeight)
    frame.setPoint(Point.TopRight, 0, index * GamesScreen.rowHeight)
    if index % 2 == 0 then frame.setBackdrop(Backdrop(Theme.background.withAlpha(0.35)))

    val cells = columns.map { at =>
      val cell = frame.createFontString()
      cell.setPoint(Point.Left, frame, Point(at, 0.5), 15, 0) // under the headers
      cell
    }

    // Like the frontend: one may join any game, unless already in one (which may then be joined back)
    val button = Button(frame)
    button.setSize(115, 34)
    button.setPoint(Point.Right, -15, 0)
    button.setScript(Script.OnClick)(_ =>
      for g <- game do if gameYouAreIn.isDefined then enter(g.id) else join(g.id)
    )

    def update(maybeGame: Option[MenuGameWithPlayers]): Unit = {
      game = maybeGame
      frame.setShown(maybeGame.isDefined)
      for g <- maybeGame do {
        val values = Seq(g.game.gameName, g.game.gameCreator.name, g.players.length.toString)
        for (cell, value) <- cells.zip(values) do cell.setText(value)
        button.setShown(gameYouAreIn.forall(_.id == g.id))
        button.setText(if gameYouAreIn.isDefined then "Join back" else "Join")
        button.setEnabled(!busy)
      }
    }
  }

  render()
  refresh()
  // Any change on the server is announced on the lobby websocket
  app.attempt(app.api.lobby(None)(_ => refresh()))(opened => lobby = Some(opened))

  override def leave(): Unit = {
    lobby.foreach(_.close())
    root.remove()
  }

  private def gameYouAreIn: Option[MenuGameWithPlayers] =
    games.flatMap(_.find(_.containsPlayer(user)))

  private def received(all: Vector[MenuGameWithPlayers]): Unit = {
    games = Some(all.filterNot(_.started))
    // Names are unique, and started games (not shown) keep theirs
    if newGameName.text.startsWith(defaultName) then {
      val taken = all.map(_.game.gameName).toSet
      newGameName.setText(
        (Iterator.single(defaultName) ++ Iterator.from(2).map(i => s"$defaultName $i"))
          .find(!taken(_))
          .get
      )
    }
    render()
  }

  /** Shows the state of the screen in its frames. */
  private def render(): Unit = {
    currentGame.setShown(gameYouAreIn.isDefined)
    for game <- gameYouAreIn do {
      currentGameText.setText(s"You are currently involved in the game '${game.game.gameName}'.")
      joinBackButton.setScript(Script.OnClick)(_ => enter(game.id))
    }
    joinBackButton.setEnabled(!busy)

    val canCreate = games.isDefined && gameYouAreIn.isEmpty
    newGameName.setShown(canCreate)
    newGameName.setEnabled(!busy)
    createButton.setShown(canCreate)
    createButton.setEnabled(!busy)

    val shown = games.getOrElse(Vector.empty)
    loading.setShown(games.isEmpty)
    noGames.setShown(games.exists(_.isEmpty))
    while rows.size < shown.size do rows += GameRow(rows.size)
    for (row, i) <- rows.zipWithIndex do row.update(shown.lift(i))
    list.scrollChild.setHeight(shown.size * GamesScreen.rowHeight)
  }

  private def create(): Unit = if !busy then {
    busy = true
    render()
    app.attempt(app.api.createGame(newGameName.text.trim))(
      game => enter(game.id),
      _ => { busy = false; render() }
    )
  }

  private def join(gameId: String): Unit = {
    busy = true
    render()
    app.attempt(app.api.joinGame(gameId))(_ => enter(gameId), _ => { busy = false; render() })
  }

  private def enter(gameId: String): Unit = app.show(new GameLobbyScreen(app, user, gameId))
}

object GamesScreen {
  private val rowHeight = 44.0
}
