package bffclient.menu

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

import bffclient.gui.*
import bffclient.hosting.HostingAgent
import bffclient.love.Timer
import bffclient.ui.{ButtonStyle, Colour, Theme}
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

  private def starting           = startingSince.isDefined
  private var gameStarted        = false
  private var left               = false
  private var preferencesApplied = false
  private val refresh            = Refresh(app, () => app.api.game(gameId))(received)

  private val hosting =
    HostingAgent(app.api, gameId, user.name, message => println(s"[menu] $message"))

  // --- Frames ---

  private val root = Frame(app.gui.root)
  root.setAllPoints()
  root.setScript(Script.OnUpdate)(_ => hosting.prefetch()) // it may be turned on in the settings

  private val loading = Templates.loading(root, "Loading the game")
  loading.setPoint(Point.TopLeft, 40, 40)

  /** Everything, once the game is loaded. */
  private val page = Frame(root)
  page.setAllPoints()

  private val title = page.createFontString(size = Theme.large)
  title.setPoint(Point.TopLeft, 40, 26)
  private val subtitle = page.createFontString(size = Theme.small, colour = Theme.muted)
  subtitle.setPoint(Point.TopLeft, 40, 58)
  page
    .createFontString(s"Logged in as ${user.name}", colour = Theme.muted)
    .setPoint(Point.Right, page, Point.TopRight, -40, 45)

  /** The left column, 60% of the width. */
  private def leftColumn(region: Region, y: Double): Unit = {
    region.setPoint(Point.TopLeft, 40, y)
    region.setPoint(Point.TopRight, page, Point(0.6, 0), 40, y)
  }

  // Players
  private val table = Templates.panel(page)
  leftColumn(table, 90)
  private val columns = Seq(0.0, 0.35, 0.58, 0.72) // where they start, in the table's width
  for (header, at) <- Seq("Name", "Class", "Colour", "Ready").zip(columns) do
    table
      .createFontString(header, colour = Theme.muted)
      .setPoint(Point.TopLeft, table, Point(at, 0), 16, 12)
  private val rows = mutable.ArrayBuffer.empty[PlayerRow]

  private final class PlayerRow(index: Int) {
    private var player = Option.empty[String]

    val frame = Frame(table)
    frame.setHeight(40)
    frame.setPoint(Point.TopLeft, 1, 42 + 40 * index)
    frame.setPoint(Point.TopRight, -1, 42 + 40 * index)
    if index % 2 == 0 then frame.setBackdrop(Backdrop(Theme.background.withAlpha(0.35)))

    private def cell[R <: Region](region: R, column: Int): R = {
      region.setPoint(Point.Left, frame, Point(columns(column), 0.5), 15, 0)
      region
    }
    private val name        = cell(frame.createFontString(), 0)
    private val playerClass = cell(frame.createFontString(), 1)
    private val colour      = cell(Swatch(frame), 2)
    private val noColour    = cell(frame.createFontString("-", colour = Theme.muted), 2)
    private val ready       = cell(frame.createFontString(), 3)
    private val kick        = Button(frame, "Kick", ButtonStyle.Danger)
    colour.setSize(22, 22)
    colour.setEnabled(false)
    kick.setSize(70, 30)
    kick.setPoint(Point.Right, -10, 0)
    kick.setScript(Script.OnClick)(_ => player.foreach(kickPlayer))

    def update(maybeInfo: Option[PlayerInfo], enabled: Boolean): Unit = {
      frame.setShown(maybeInfo.isDefined)
      for info <- maybeInfo do {
        player = Some(info.playerName.name)
        name.setText(info.playerName match {
          case PlayerName.HumanPlayerName(name) =>
            if name == user.name then s"$name (you)" else name
          case ai: PlayerName.AIPlayerName => s"${ai.name} (AI)"
        })
        playerClass.setText(info.maybePlayerClass.fold("Not chosen")(_.value))
        playerClass.setColour(if info.maybePlayerClass.isDefined then Theme.text else Theme.muted)
        info.maybePlayerColour.foreach(c => colour.setColour(MenuColours.toUi(c)))
        colour.setShown(info.maybePlayerColour.isDefined)
        noColour.setShown(info.maybePlayerColour.isEmpty)
        ready.setText(if info.isReady then "Ready" else "Not ready")
        ready.setColour(
          if info.isReady then Theme.positive.lighter(0.3) else Theme.negative.lighter(0.3)
        )
        kick.setShown(amICreator && info.playerName.name != user.name)
        kick.setEnabled(enabled)
      }
    }
  }

  // My settings
  private val mine = Templates.panel(page)
  mine.setPoint(Point.TopLeft, table, Point.BottomLeft, 0, 20)
  mine.setPoint(Point.TopRight, table, Point.BottomRight, 0, 20)
  mine.setHeight(150)
  mine.createFontString("Class", colour = Theme.muted).setPoint(Point.TopLeft, 16, 18)
  private val classes = Segmented[PlayerClasses](mine, _.value)
  classes.setSize(420, 36)
  classes.setPoint(Point.TopLeft, 100, 10)
  classes.setScript(classes.OnValueChanged)(cls => changeMe(_.withClass(Some(cls))))
  mine.createFontString("Colour", colour = Theme.muted).setPoint(Point.TopLeft, 16, 64)
  private val swatches = MenuColours.palette.zipWithIndex.map { (colour, i) =>
    val swatch = Swatch(mine, MenuColours.toUi(colour))
    swatch.setSize(24, 24)
    swatch.setPoint(Point.TopLeft, 100 + 30 * i, 60)
    swatch.setScript(Script.OnClick)(_ => changeMe(_.withColour(colour)))
    colour -> swatch
  }
  private val readyCheck = CheckButton(mine, "Ready")
  readyCheck.setPoint(Point.TopLeft, 16, 108)
  readyCheck.setScript(Script.OnClick)(_ =>
    changeMe(
      _.withReadyState(if readyCheck.isChecked then PlayerStatus.Ready else PlayerStatus.NotReady)
    )
  )

  // What prevents the game to start
  private val problemsTitle = page.createFontString()
  private val problems      = page.createFontString(size = Theme.small, colour = Theme.muted)
  problems.setPoint(Point.TopLeft, problemsTitle, Point.BottomLeft, 16, 5)

  // What the boss and the classes do, like the frontend's `GameDocs`
  private val docsPanel = DocsPanel(page, changeBoss)
  docsPanel.frame.setPoint(Point.TopLeft, page, Point(0.6, 0), 70, 90)
  docsPanel.frame.setPoint(Point.BottomRight, -40, -90)

  // Actions
  private def action(button: Button, width: Double, point: Point, x: Double): Button = {
    button.setSize(width, 44)
    button.setPoint(point, x, -26)
    button
  }
  private val leaveButton = action(Button(page, "Leave game"), 150, Point.BottomLeft, 40)
  leaveButton.setScript(Script.OnClick)(_ => leaveGame())
  private val settingsButton = action(Button(page, "Settings"), 130, Point.BottomLeft, 200)
  settingsButton.setScript(Script.OnClick)(_ => app.showSettings())
  DownloadIndicator(page).frame.setPoint(Point.TopLeft, page, Point.BottomLeft, 350, -64)

  private val addAIButton = action(Button(page, "Add AI"), 150, Point.BottomRight, -410)
  addAIButton.setScript(Script.OnClick)(_ => addAIs(1))
  private val fillButton = action(Button(page, "Fill with AIs"), 170, Point.BottomRight, -230)
  fillButton.setScript(Script.OnClick)(_ => addAIs(aisToFill))
  private val launchButton =
    action(Button(page, "Launch game!", ButtonStyle.Positive), 180, Point.BottomRight, -40)
  launchButton.setScript(Script.OnClick) { _ =>
    launching = true
    render()
    app.attempt(app.api.startGame(gameId))(_ => (), _ => { launching = false; render() })
  }
  private val waitingText =
    page.createFontString("Waiting for the game creator to launch the game", colour = Theme.muted)
  waitingText.setPoint(Point.Right, page, Point.BottomRight, -40, -48)

  // While the game starts, over everything
  private val startingOverlay = Frame(root)
  startingOverlay.setAllPoints()
  startingOverlay.setFrameStrata(Strata.Dialog)
  startingOverlay.setBackdrop(Backdrop(Theme.background.withAlpha(0.8)))
  startingOverlay.enableMouse(true) // nothing under it can be clicked
  Templates
    .loading(startingOverlay, "Game is starting, please wait for setup to finish")
    .setPoint(Point.TopLeft, startingOverlay, Point.Center, -240, 0)
  private val hostingStatus = startingOverlay.createFontString(colour = Theme.muted)
  hostingStatus.setPoint(Point.Center, 0, 55)
  DownloadIndicator(startingOverlay).frame
    .setPoint(Point.TopLeft, startingOverlay, Point.Center, -130, 80)
  // the host may have to download the game server first (and someone else may take over)
  private val notReady =
    startingOverlay.createFontString("The game server is not ready yet.", colour = Theme.muted)
  notReady.setPoint(Point.Center, 0, 135)
  private val leaveWhileStarting = Button(startingOverlay, "Leave game")
  leaveWhileStarting.setSize(150, 44)
  leaveWhileStarting.setPoint(Point.TopLeft, startingOverlay, Point.Center, -75, 160)
  leaveWhileStarting.setScript(Script.OnClick)(_ => leaveGame())
  startingOverlay.setScript(Script.OnUpdate) { _ =>
    hostingStatus.setText(hosting.status.getOrElse(""))
    val late = startingSince.exists(Timer.getTime() - _ > 60)
    notReady.setShown(late)
    leaveWhileStarting.setShown(late)
    leaveWhileStarting.setEnabled(!busy)
  }

  /** What is being downloaded to run the game server, discreetly: what it is and how much of it is
    * there, above a thin bar. Its frame is 260 x 32.
    */
  private final class DownloadIndicator(parent: Frame) {
    val frame = Frame(parent)
    frame.setSize(260, 32)
    // the frame stays shown, for its OnUpdate
    private val content = Frame(frame)
    content.setAllPoints()
    private val what = content.createFontString(size = Theme.small, colour = Theme.muted)
    what.setPoint(Point.TopLeft)
    private val amount = content.createFontString(size = Theme.small, colour = Theme.muted)
    amount.setPoint(Point.Right, content, Point.BottomRight, 0, -3)
    private val bar = StatusBar(content)
    bar.setPoint(Point.TopLeft, content, Point.BottomLeft, 0, -5)
    bar.setPoint(Point.BottomRight, -88, 0)
    frame.setScript(Script.OnUpdate)(_ =>
      hosting.downloading match {
        case Some(download) =>
          def megabytes(bytes: Double) = (bytes / 1000000).toInt
          content.show()
          what.setText(s"Downloading ${download.what}")
          amount.setText(
            s"${megabytes(download.received)} / ${megabytes(download.size.toDouble)} MB"
          )
          bar.setValue(download.fraction)
        case None => content.hide()
      }
    )
  }

  render()
  refresh()
  app.attempt(app.api.lobby(Some(gameId)) {
    case MenuGameComm.DataUpdated() => refresh()
    case MenuGameComm.GameStarted() =>
      startingSince = Some(Timer.getTime())
      render()
    case MenuGameComm.GameLaunchFailed(reason) =>
      startingSince = None
      launching = false
      hosting.status = None
      app.notify(reason, isError = true)
      render()
      refresh()
    case credentials: MenuGameComm.HereAreYourCredentials =>
      gameStarted = true
      app.gameStarts(app.api.gameAccess(user.name, credentials), user, lobby)
    case other => hosting.handle(other)
  })(opened => lobby = Some(opened))

  // Once the game started, the lobby websocket is closed by MenuApp when coming back to the menus
  override def leave(): Unit = {
    left = true
    if !gameStarted then lobby.foreach(_.close())
    root.remove()
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
      render()
      if !preferencesApplied then { preferencesApplied = true; applyPreferences(g) }
  }

  private def config: Option[GameConfiguration] = game.map(_.game.gameConfiguration)
  private def me: Option[PlayerInfo]            = config.flatMap(_.playersInfo.get(user.name))
  private def amICreator: Boolean               = game.exists(_.isGameCreator(user))
  private def boss: Option[BossMetadata] =
    config.flatMap(c => BossMetadata.maybeMetadataByName(c.bossName))

  /** How many AIs the boss still needs (like the menu server, which picks the classes of the AIs:
    * all the players' classes count).
    */
  private def aisToFill: Int = {
    val classes = config.toVector.flatMap(_.playersInfo.values.flatMap(_.maybePlayerClass))
    boss.flatMap(_.fillWithAI(classes)).fold(0)(_.size)
  }

  /** Shows the state of the screen in its frames. */
  private def render(): Unit = {
    loading.setShown(game.isEmpty)
    page.setShown(game.isDefined)
    startingOverlay.setShown(starting)
    for g <- game do {
      val configuration = g.game.gameConfiguration
      val enabled       = !busy && !starting
      title.setText(g.game.gameName)
      subtitle.setText(s"Created by ${g.game.gameCreator.name} - game ${g.id}")

      val players = configuration.playersInfo.values.toVector.sortBy(info =>
        (info.playerName.name != user.name, info.playerName.name)
      )
      table.setHeight(44 + 40 * math.max(players.size, 1))
      while rows.size < players.size do rows += PlayerRow(rows.size)
      for (row, i) <- rows.zipWithIndex do row.update(players.lift(i), enabled)

      mine.setShown(me.isDefined)
      for info <- me do {
        classes.setOptions(PlayerClasses.allChoices, info.maybePlayerClass)
        classes.setEnabled(enabled)
        for (colour, swatch) <- swatches do {
          swatch.setSelected(info.maybePlayerColour.contains(colour))
          swatch.setEnabled(enabled)
        }
        readyCheck.setChecked(info.isReady)
        readyCheck.setEnabled(enabled)
      }

      val above = if me.isDefined then mine else table
      problemsTitle.setPoint(Point.TopLeft, above, Point.BottomLeft, 0, 20)
      val gameProblems = configuration.validate.toVector.sortBy(_._1)
      problemsTitle.setText(
        if gameProblems.isEmpty then "Everyone is ready and well set, LFG"
        else "The following things prevent the game to start:"
      )
      problemsTitle.setColour(
        if gameProblems.isEmpty then Theme.positive.lighter(0.3) else Theme.text
      )
      problems.setText(
        gameProblems.map((name, errors) => s"$name: ${errors.mkString(", ")}").mkString("\n")
      )

      docsPanel.update(configuration.bossName, amICreator, enabled, me.flatMap(_.maybePlayerClass))

      leaveButton.setEnabled(enabled)
      for button <- Seq(addAIButton, fillButton) do {
        button.setShown(amICreator)
        button.setEnabled(enabled && aisToFill > 0)
      }
      launchButton.setShown(amICreator)
      launchButton.setEnabled(enabled && !launching && g.game.gameConfigurationIsValid)
      waitingText.setShown(!amICreator)
    }
  }

  private def setBusy(busy: Boolean): Unit = {
    this.busy = busy
    render()
  }

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
    do changeBoss(bossName)
  }

  private def changeMe(f: PlayerInfo => PlayerInfo): Unit = me.foreach { info =>
    val changed = f(info)
    setBusy(true)
    app.attempt(app.api.changePlayerInfo(gameId, changed))(
      updated => {
        game = Some(updated)
        setBusy(false)
        app.updatePreferences(
          _.copy(playerClass = changed.maybePlayerClass, colour = changed.maybePlayerColour)
        )
      },
      _ => { setBusy(false); refresh() }
    )
  }

  private def changeBoss(bossName: String): Unit = {
    setBusy(true)
    app.attempt(app.api.changeGameConfig(gameId, GameConfiguration.GameConfigMetadata(bossName)))(
      updated => {
        game = Some(updated)
        setBusy(false)
        app.updatePreferences(_.copy(bossName = Some(bossName)))
      },
      _ => setBusy(false)
    )
  }

  private def addAIs(count: Int): Unit = {
    setBusy(true)
    val added = (1 to count).foldLeft(Future.unit)((previous, _) =>
      previous.flatMap(_ => app.api.addAI(gameId).map(_ => ()))
    )
    app.attempt(added)(_ => setBusy(false), _ => setBusy(false))
  }

  private def kickPlayer(name: String): Unit = {
    setBusy(true)
    app.attempt(app.api.kickPlayer(gameId, name))(_ => setBusy(false), _ => setBusy(false))
  }

  private def leaveGame(): Unit = {
    setBusy(true)
    app.attempt(app.api.leaveGame(gameId))(
      _ => app.show(new GamesScreen(app, user)),
      _ => setBusy(false)
    )
  }
}

object MenuColours {
  val palette: Vector[RGBColour] = RGBColour.someColours

  def toUi(colour: RGBColour): Colour = Colour.rgb(colour.red, colour.green, colour.blue)
}
