package bffclient.menu

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

import bffclient.controls.SettingsEditor
import bffclient.game.{GameAccess, GameExit}
import bffclient.gui.*
import bffclient.love.{Fullscreen, Graphics, Keyboard, Love, Timer}
import bffclient.ui.Theme
import menus.data.User

/** A screen of the menus: frames of [[MenuApp.gui]], created with the screen. */
trait Screen {

  /** When the menus switch to another screen: it removes its frames. */
  def leave(): Unit
}

/** "Loading..." while waiting for something. */
final class LoadingScreen(app: MenuApp, message: String) extends Screen {
  private val frame = Templates.loading(app.gui.root, message)
  frame.setPoint(Point.TopLeft, 40, 40)

  def leave(): Unit = frame.remove()
}

/** The menus, made with [[Gui]] like the frontend's pages: login, games, then the game's lobby,
  * until the game server credentials arrive and `startGame` takes over. The game comes back to the
  * menus with the function it is given.
  *
  * The menu server is the one typed on the login screen (`server` fills it in, otherwise the last
  * one used, otherwise the build's default, `BuildConfig.menuServer`).
  */
final class MenuApp(val server: Option[String], startGame: (GameAccess, GameExit => Unit) => Unit)(
    using ExecutionContext
) {
  val gui                 = new Gui
  var preferences         = MenuPreferences.load()
  private var connectedTo = Option.empty[MenuApi]

  /** The menu server we logged in to. */
  def api: MenuApi                  = connectedTo.getOrElse(sys.error("Not logged in"))
  def useServer(api: MenuApi): Unit = connectedTo = Some(api)
  private var screen: Screen        = new LoginScreen(this)

  gui.root
    .createFontString("F11: fullscreen", Theme.small, Theme.muted)
    .setPoint(Point.TopLeft, gui.root, Point.BottomRight, -140, -22)

  /** Over everything, for 5 seconds (long ones wrap at the window's width). */
  private val notification = Frame(gui.root)
  notification.setFrameStrata(Strata.Tooltip)
  notification.setPoint(Point.Center, gui.root, Point.Bottom, 0, -90)
  notification.hide()
  private val notificationText = notification.createFontString()
  notificationText.setWordWrap(true)
  notificationText.setJustifyH(JustifyH.Center)
  notificationText.setPoint(Point.TopLeft, 20, 10)
  notificationText.setPoint(Point.TopRight, -20, 10)
  private var notificationUntil = 0.0
  notification.setScript(Script.OnUpdate)(_ =>
    if Timer.getTime() > notificationUntil then notification.hide()
  )

  /** The settings, over the current screen (which stays open, e.g. the lobby and its subscription).
    */
  private var settingsEditor = Option.empty[SettingsEditor]

  def showSettings(): Unit = if settingsEditor.isEmpty then
    settingsEditor = Some(SettingsEditor(gui.root, () => settingsEditor = None))

  def install(): Unit = {
    Keyboard.setKeyRepeat(true)
    Love.draw = () => {
      Graphics.setBackgroundColor(Theme.background.r, Theme.background.g, Theme.background.b)
      gui.draw()
    }
    Love.wheelmoved = (_, dy) => gui.wheelMoved(dy)
    Love.mousepressed = (x, y, button, _, _) => gui.mousePressed(x, y, button)
    Love.mousereleased = (x, y, button, _, _) => gui.mouseReleased(x, y, button)
    Love.textinput = text => gui.textInput(text)
    Love.keypressed = (_, scancode, isRepeat) =>
      if scancode == Fullscreen.key then { if !isRepeat then Fullscreen.toggle() }
      else gui.keyPressed(scancode)
  }

  def show(next: Screen): Unit = { screen.leave(); screen = next }

  def notify(message: String, isError: Boolean = false): Unit = {
    println(s"[menu] $message")
    notificationText.setText(message)
    notification.setWidth(math.min(notificationText.stringWidth, gui.root.width - 120) + 40)
    notification.setHeight(notificationText.height + 20)
    notification.setBackdrop(Backdrop(if isError then Theme.negative else Theme.positive))
    notificationUntil = Timer.getTime() + 5
    notification.show()
  }

  /** Calls `onSuccess` with the result, or notifies the failure (and calls `onFailure`). */
  def attempt[A](
      future: Future[A]
  )(onSuccess: A => Unit, onFailure: Throwable => Unit = _ => ()): Unit =
    future.onComplete {
      case Success(a) => onSuccess(a)
      case Failure(error) =>
        notify(error.getMessage, isError = true)
        onFailure(error)
    }

  def updatePreferences(f: MenuPreferences => MenuPreferences): Unit = {
    preferences = f(preferences)
    preferences.save()
  }

  /** The lobby stays subscribed during the game: the menu server kicks the players who are not.
    */
  def gameStarts(access: GameAccess, user: User, lobby: Option[Lobby]): Unit = {
    settingsEditor.foreach(_.frame.remove())
    settingsEditor = None
    // the lobby's screen leaves it subscribed, now that the game started
    show(LoadingScreen(this, "Connecting to the game server"))
    startGame(access, exit => backToMenus(user, access.gameId, lobby, exit))
  }

  /** The menu server never ends games by itself (players stay "already playing" until it sees them
    * disconnected from the lobby for a while), so we leave the game, which deletes it for its
    * creator. The game server stops by itself once everyone left it.
    */
  private def backToMenus(
      user: User,
      gameId: String,
      lobby: Option[Lobby],
      exit: GameExit
  ): Unit = {
    Love.update = null
    Love.keyreleased = null
    install()
    exit match {
      // the game server stops by itself once everyone left
      case GameExit.Ended | GameExit.LeftEarly(_) => ()
      case GameExit.ConnectionLost =>
        notify("The connection to the game server was lost", isError = true)
      case GameExit.CannotConnect(error) =>
        notify(s"Cannot connect to the game server: $error", isError = true)
    }
    lobby.foreach(_.close())
    show(LoadingScreen(this, "Leaving the game"))
    api.leaveGame(gameId).onComplete { result =>
      result.failed.foreach(failure =>
        notify(s"Could not leave the game: ${failure.getMessage}", isError = true)
      )
      show(new GamesScreen(this, user))
    }
  }
}
