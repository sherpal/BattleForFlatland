package bffclient.menu

import scala.concurrent.{ExecutionContext, Future}

import bffclient.game.GameAccess
import bffclient.love.Timer
import bffclient.ui.*
import gamelogic.docs.BossMetadata
import menus.data.{MenuGameComm, User}
import models.bff.outofgame.{MenuGameWithPlayers, PlayerClasses}
import models.bff.outofgame.gameconfig.*
import utils.misc.RGBColour

/** The settings of a game before it starts, like the frontend's `GameSettings`: boss, players,
  * one's class, colour and readiness. The game creator adds AIs and launches the game.
  */
final class GameLobbyScreen(app: MenuApp, user: User, gameId: String)(using ExecutionContext)
    extends Screen {
  private var game          = Option.empty[MenuGameWithPlayers]
  private var lobby         = Option.empty[Lobby]
  private var busy          = false
  private var launching     = false
  private var startingSince = Option.empty[Double]

  private val docsPanel = new DocsPanel
  private def starting           = startingSince.isDefined
  private var gameStarted        = false
  private var left               = false
  private var preferencesApplied = false
  private val refresh            = Refresh(app, () => app.api.game(gameId))(received)

  refresh()
  app.attempt(app.api.lobby(Some(gameId)) {
    case MenuGameComm.DataUpdated() => refresh()
    case MenuGameComm.GameStarted() => startingSince = Some(Timer.getTime())
    case MenuGameComm.HereAreYourCredentials(id, secret, port) =>
      gameStarted = true
      app.gameStarts(GameAccess(app.api.host, port, user.name, id, secret), user, lobby)
  })(opened => lobby = Some(opened))

  // Once the game started, the lobby websocket is closed by MenuApp when coming back to the menus
  override def leave(): Unit = {
    left = true
    if !gameStarted then lobby.foreach(_.close())
  }

  private def received(maybeGame: Option[MenuGameWithPlayers]): Unit = maybeGame match {
    case _ if left || gameStarted => () // answers to requests made before
    case None =>
      app.notify("The game was closed by its creator", isError = true)
      app.show(new GamesScreen(app, user))
    case Some(g) if !g.containsPlayer(user) =>
      app.notify("You are not in this game (anymore)", isError = true)
      app.show(new GamesScreen(app, user))
    case Some(g) =>
      game = Some(g)
      if !preferencesApplied then { preferencesApplied = true; applyPreferences(g) }
  }

  private def config: Option[GameConfiguration] = game.map(_.game.gameConfiguration)
  private def me: Option[PlayerInfo]            = config.flatMap(_.playersInfo.get(user.name))
  private def amICreator: Boolean               = game.exists(_.isGameCreator(user))
  private def boss: Option[BossMetadata]        =
    config.flatMap(c => BossMetadata.maybeMetadataByName(c.bossName))

  /** Like the frontend, the last class, colour (and boss for the creator) are chosen again. */
  private def applyPreferences(g: MenuGameWithPlayers): Unit = {
    val preferences = app.preferences
    for info <- me do {
      val wanted = info
        .withClass(preferences.playerClass.orElse(info.maybePlayerClass))
        .copy(maybePlayerColour = preferences.colour.orElse(info.maybePlayerColour))
        .withReadyState(PlayerStatus.NotReady)
      if wanted != info then changeMe(_ => wanted)
    }
    for
      bossName <- preferences.bossName
      if amICreator && bossName != g.game.gameConfiguration.bossName
    do
      changeBoss(bossName)
  }

  private def changeMe(f: PlayerInfo => PlayerInfo): Unit = me.foreach { info =>
    val changed = f(info)
    busy = true
    app.attempt(app.api.changePlayerInfo(gameId, changed))(
      updated => {
        busy = false
        game = Some(updated)
        app.updatePreferences(
          _.copy(playerClass = changed.maybePlayerClass, colour = changed.maybePlayerColour)
        )
      },
      _ => { busy = false; refresh() }
    )
  }

  private def changeBoss(bossName: String): Unit = {
    busy = true
    app.attempt(app.api.changeGameConfig(gameId, GameConfiguration.GameConfigMetadata(bossName)))(
      updated => {
        busy = false; game = Some(updated); app.updatePreferences(_.copy(bossName = Some(bossName)))
      },
      _ => busy = false
    )
  }

  private def addAIs(count: Int): Unit = {
    busy = true
    val added = (1 to count).foldLeft(Future.unit)((previous, _) =>
      previous.flatMap(_ => app.api.addAI(gameId).map(_ => ()))
    )
    app.attempt(added)(_ => busy = false, _ => busy = false)
  }

  def draw(ui: Ui): Unit = game match {
    case None => ui.loading("Loading the game", 40, 40)
    case Some(g) => drawGame(ui, g)
  }

  private def drawGame(ui: Ui, g: MenuGameWithPlayers): Unit = {
    val (width, height) = app.size
    val configuration   = g.game.gameConfiguration
    val enabled         = !busy && !starting
    val left            = 40.0
    val leftWidth       = width * 0.6
    val right           = left + leftWidth + 30
    val rightWidth      = width - right - 40

    ui.text(g.game.gameName, left, 26, Theme.large)
    ui.text(
      s"Created by ${g.game.gameCreator.name} - game ${g.id}",
      left,
      58,
      Theme.small,
      Theme.muted
    )
    ui.textIn(
      s"Logged in as ${user.name}",
      Rect(0, 30, width - 40, 30),
      Theme.body,
      Theme.muted,
      "right"
    )

    // Players
    val players = configuration.playersInfo.values.toVector.sortBy(info =>
      (info.playerName.name != user.name, info.playerName.name)
    )
    val table = Rect(left, 90, leftWidth, 44 + 40 * math.max(players.size, 1))
    ui.panel(table)
    val columns = Seq("Name" -> 0.0, "Class" -> 0.35, "Colour" -> 0.58, "Ready" -> 0.72)
    for (title, at) <- columns do
      ui.text(title, table.x + 16 + at * table.width, table.y + 12, colour = Theme.muted)
    for (info, i) <- players.zipWithIndex do {
      val row = Rect(table.x + 1, table.y + 42 + 40 * i, table.width - 2, 40)
      if i % 2 == 0 then ui.fill(row, Theme.background.withAlpha(0.35))
      def cell(at: Double) =
        Rect(table.x + 16 + at * table.width, row.y, table.width * 0.2, row.height)
      val name = info.playerName match {
        case PlayerName.HumanPlayerName(name) => if name == user.name then s"$name (you)" else name
        case ai: PlayerName.AIPlayerName => s"${ai.name} (AI)"
      }
      ui.textIn(name, cell(columns(0)._2), align = "left")
      ui.textIn(
        info.maybePlayerClass.fold("Not chosen")(_.value),
        cell(columns(1)._2),
        align = "left",
        colour = if info.maybePlayerClass.isDefined then Theme.text else Theme.muted
      )
      info.maybePlayerColour match {
        case Some(colour) =>
          ui.swatch(
            MenuColours.toUi(colour),
            Rect(cell(columns(2)._2).x, row.y + 9, 22, 22),
            enabled = false
          )
        case None => ui.textIn("-", cell(columns(2)._2), align = "left", colour = Theme.muted)
      }
      ui.textIn(
        if info.isReady then "Ready" else "Not ready",
        cell(columns(3)._2),
        align = "left",
        colour = if info.isReady then Theme.positive.lighter(0.3) else Theme.negative.lighter(0.3)
      )
      if
        amICreator && info.playerName.name != user.name &&
        ui.button("Kick", Rect(row.right - 80, row.y + 5, 70, 30), enabled, ButtonStyle.Danger)
      then {
        busy = true
        app.attempt(app.api.kickPlayer(gameId, info.playerName.name))(
          _ => busy = false,
          _ => busy = false
        )
      }
    }

    // My settings
    var y = table.bottom + 20
    for info <- me do {
      val panel = Rect(left, y, leftWidth, 150)
      ui.panel(panel)
      ui.text("Class", panel.x + 16, panel.y + 18, colour = Theme.muted)
      ui.segmented(
        PlayerClasses.allChoices,
        info.maybePlayerClass,
        Rect(panel.x + 100, panel.y + 10, 420, 36),
        (_: PlayerClasses).value,
        enabled
      ).foreach(cls => changeMe(_.withClass(Some(cls))))
      ui.text("Colour", panel.x + 16, panel.y + 64, colour = Theme.muted)
      for (colour, i) <- MenuColours.palette.zipWithIndex do
        if
          ui.swatch(
            MenuColours.toUi(colour),
            Rect(panel.x + 100 + 30 * i, panel.y + 60, 24, 24),
            info.maybePlayerColour.contains(colour),
            enabled
          )
        then
          changeMe(_.withColour(colour))
      if ui.checkbox(info.isReady, "Ready", panel.x + 16, panel.y + 108, enabled) then
        changeMe(_.withReadyState(if info.isReady then PlayerStatus.NotReady else PlayerStatus.Ready))
      y = panel.bottom + 20
    }

    // What prevents the game to start
    val problems = configuration.validate.toVector.sortBy(_._1)
    if problems.isEmpty then
      ui.text("Everyone is ready and well set, LFG", left, y, colour = Theme.positive.lighter(0.3))
    else {
      ui.text("The following things prevent the game to start:", left, y)
      for ((name, errors), i) <- problems.zipWithIndex do
        ui.text(
          s"$name: ${errors.mkString(", ")}",
          left + 16,
          y + 24 + 20 * i,
          Theme.small,
          Theme.muted
        )
    }

    // What the boss and the classes do, like the frontend's `GameDocs`
    docsPanel.draw(
      ui,
      Rect(right, 90, rightWidth, height - 180),
      configuration.bossName,
      Option.when[String => Unit](amICreator)(changeBoss),
      enabled,
      me.flatMap(_.maybePlayerClass)
    )

    // Actions
    val bar = height - 70
    if ui.button("Leave game", Rect(left, bar, 150, 44), enabled) then leaveGame()
    if ui.button("Controls", Rect(left + 160, bar, 130, 44)) then app.showControls()
    if amICreator then {
      // Like the menu server, which picks the classes of the AIs: all the players' classes count
      val classes = configuration.playersInfo.values.flatMap(_.maybePlayerClass).toVector
      val toFill  = boss.flatMap(_.fillWithAI(classes)).fold(0)(_.size)
      if ui.button("Add AI", Rect(width - 560, bar, 150, 44), enabled && toFill > 0) then addAIs(1)
      if ui.button("Fill with AIs", Rect(width - 400, bar, 170, 44), enabled && toFill > 0) then
        addAIs(toFill)
      if
        ui.button(
          "Launch game!",
          Rect(width - 220, bar, 180, 44),
          enabled && !launching && g.game.gameConfigurationIsValid,
          ButtonStyle.Positive
        )
      then {
        launching = true
        app.attempt(app.api.startGame(gameId))(_ => (), _ => launching = false)
      }
    } else
      ui.textIn(
        "Waiting for the game creator to launch the game",
        Rect(0, bar, width - 40, 44),
        colour = Theme.muted,
        align = "right"
      )

    for since <- startingSince do {
      ui.fill(Rect(0, 0, width, height), Theme.background.withAlpha(0.8))
      ui.loading("Game is starting, please wait for setup to finish", width / 2 - 240, height / 2)
      if Timer.getTime() - since > 30 then {
        ui.textIn(
          "The game server did not answer.",
          Rect(0, height / 2 + 40, width, 30),
          colour = Theme.muted
        )
        if ui.button("Leave game", Rect(width / 2 - 75, height / 2 + 80, 150, 44), !busy) then
          leaveGame()
      }
    }
  }

  private def leaveGame(): Unit = {
    busy = true
    app.attempt(app.api.leaveGame(gameId))(
      _ => app.show(new GamesScreen(app, user)),
      _ => busy = false
    )
  }
}

object MenuColours {
  val palette: Vector[RGBColour] = RGBColour.someColours

  def toUi(colour: RGBColour): Colour = Colour.rgb(colour.red, colour.green, colour.blue)
}
