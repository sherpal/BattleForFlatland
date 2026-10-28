package bffclient.menu

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

import bffclient.controls.SettingsEditor
import bffclient.game.{GameAccess, GameExit}
import bffclient.love.{Fullscreen, Graphics, Keyboard, Love, Timer}
import bffclient.ui.*
import menus.data.User

/** A screen of the menus, drawn every frame. */
trait Screen {
  def draw(ui: Ui): Unit

  /** When the menus switch to another screen. */
  def leave(): Unit = ()
}

/** The menus, drawn with [[Ui]] like the frontend's pages: login, games, then the game's lobby,
  * until the game server credentials arrive and `startGame` takes over. The game comes back to the
  * menus with the function it is given.
  *
  * The menu server is the one typed on the login screen (`server` fills it in, otherwise the last
  * one used).
  */
final class MenuApp(val server: Option[String], startGame: (GameAccess, GameExit => Unit) => Unit)(
    using ExecutionContext
) {
  val ui                  = new Ui
  var preferences         = MenuPreferences.load()
  private var connectedTo = Option.empty[MenuApi]

  /** The menu server we logged in to. */
  def api: MenuApi                  = connectedTo.getOrElse(sys.error("Not logged in"))
  def useServer(api: MenuApi): Unit = connectedTo = Some(api)
  private var screen: Screen        = new LoginScreen(this)
  private var notification: Option[(String, Boolean, Double)] = None

  /** The settings, over the current screen (which stays open, e.g. the lobby and its websocket). */
  private var settingsEditor = Option.empty[SettingsEditor]

  def showSettings(): Unit = settingsEditor = Some(SettingsEditor(() => settingsEditor = None))

  def install(): Unit = {
    Keyboard.setKeyRepeat(true)
    Love.draw = () => {
      Graphics.setBackgroundColor(Theme.background.r, Theme.background.g, Theme.background.b)
      ui.frame {
        settingsEditor match {
          case Some(editor) =>
            ui.inert(screen.draw(ui))
            val (width, height) = size
            editor.draw(ui, width, height)
          case None => screen.draw(ui)
        }
        drawNotification()
        val (width, height) = size
        ui.text("F11: fullscreen", width - 140, height - 22, Theme.small, Theme.muted)
      }
    }
    Love.wheelmoved = (_, dy) => ui.wheelMoved(dy)
    Love.mousepressed = (x, y, button, _, _) => ui.mousePressed(x, y, button)
    Love.mousereleased = (x, y, button, _, _) => ui.mouseReleased(x, y, button)
    Love.textinput = text => ui.textInput(text)
    Love.keypressed = (_, scancode, isRepeat) =>
      if scancode == Fullscreen.key then { if !isRepeat then Fullscreen.toggle() }
      else ui.keyPressed(scancode)
  }

  def show(next: Screen): Unit = { screen.leave(); screen = next }

  def size: (Double, Double) = {
    val dimensions = Graphics.getDimensions()
    (dimensions.width.toDouble, dimensions.height.toDouble)
  }

  def notify(message: String, isError: Boolean = false): Unit = {
    println(s"[menu] $message")
    notification = Some((message, isError, Timer.getTime() + 5))
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

  /** The lobby websocket stays open during the game: the menu server kicks the players who leave
    * it.
    */
  def gameStarts(access: GameAccess, user: User, lobby: Option[Lobby]): Unit = {
    settingsEditor = None
    screen = new Screen {
      def draw(ui: Ui): Unit = ui.loading("Connecting to the game server", 40, 40)
    }
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
    screen = new Screen { def draw(ui: Ui): Unit = ui.loading("Leaving the game", 40, 40) }
    api.leaveGame(gameId).onComplete { result =>
      result.failed.foreach(failure =>
        notify(s"Could not leave the game: ${failure.getMessage}", isError = true)
      )
      show(new GamesScreen(this, user))
    }
  }

  private def drawNotification(): Unit = notification.foreach { (message, isError, until) =>
    if Timer.getTime() > until then notification = None
    else {
      val (width, height) = size
      val textWidth       = ui.textWidth(message)
      val rect            = Rect((width - textWidth) / 2 - 20, height - 110, textWidth + 40, 40)
      ui.fill(rect, if isError then Theme.negative else Theme.positive)
      ui.textIn(message, rect)
    }
  }
}
