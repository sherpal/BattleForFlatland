package bffclient

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import scala.util.control.NonFatal

import bffclient.controls.{ClientSettings, ControlsFile}
import bffclient.game.{GameAccess, GameClient, GameExit, GameLink}
import bffclient.hud.{EndOfGame, GameMenu}
import bffclient.love.{Event, Fullscreen, Love}
import bffclient.lua.Os
import bffclient.menu.{MenuApi, MenuApp, MenuSession}
import bffclient.render.Renderer
import gamelogic.physics.Complex
import models.bff.outofgame.PlayerClasses
import bffclient.gui.Script
import communication.udp.Peer

/** The LÖVE client of Battle for Flatland: the menus, then the game ([[GameClient]]), drawn by the
  * [[Renderer]] with its HUD and overlays.
  *
  * Configuration via environment variables (see [[main]]). The menus are the default in a window.
  * With BFF_HEADLESS set, there is no window: the client walks a bit, logs, and quits at the end.
  */
object Main {
  given ExecutionContext = ExecutionContext.global

  private def env(name: String, default: String): String =
    Option(Os.getenv(name)).getOrElse(default)

  /** In a window, starts with the menus' UI, where the player types the menu server's address
    * (BFF_MENU_URL, e.g. http://127.0.0.1:9000, fills it in). Headless, plays the menus with a
    * script (BFF_MENU_URL, BFF_USER, BFF_CLASS, BFF_BOSS). With BFF_HOST (or headless without
    * BFF_MENU_URL), connects directly to a game server (BFF_HOST, BFF_PORT, BFF_SECRET,
    * BFF_GAME_ID).
    */
  def main(args: Array[String]): Unit = {
    val user     = env("BFF_USER", "alice")
    val headless = Os.getenv("BFF_HEADLESS") != null
    val menuUrl  = Option(Os.getenv("BFF_MENU_URL"))
    menuUrl match {
      case Some(url) if headless =>
        val (scheme, host, port) =
          MenuApi.parseAddress(url).fold(message => sys.error(s"BFF_MENU_URL: $message"), identity)
        val api = MenuApi(scheme, host, port)
        val playerClass = PlayerClasses
          .playerClassByName(env("BFF_CLASS", "Triangle"))
          .getOrElse(PlayerClasses.Triangle)
        val menu = MenuSession(api, user, playerClass, env("BFF_BOSS", "Boss 101"))
        menu.run().onComplete {
          case Success(access) => startGame(access, headless)
          case Failure(error) =>
            menu.info(s"ERROR: ${error.getMessage}")
            Event.quit(1)
        }
      case None if headless || Os.getenv("BFF_HOST") != null =>
        startGame(
          GameAccess(
            Vector(Peer(env("BFF_HOST", "localhost"), env("BFF_PORT", "22222").toInt)),
            None,
            user,
            env("BFF_GAME_ID", "game"),
            env("BFF_SECRET", "secret")
          ),
          headless
        )
      case _ =>
        MenuApp(
          menuUrl,
          (access, backToMenus) => startGame(access, headless, Some(backToMenus))
        ).install()
    }
  }

  /** Connects to the game server (through UDP) and runs the game. When the game was joined through
    * the menus, `backToMenus` goes back to them.
    */
  def startGame(
      access: GameAccess,
      headless: Boolean,
      backToMenus: Option[GameExit => Unit] = None
  ): Unit = {
    println(s"Connecting to the game server at ${access.addresses.mkString(", ")} (UDP)")
    val lag = env("BFF_LAG_MS", "0").toDouble / 1000
    if lag > 0 then println(s"Simulated latency: ${lag * 1000} ms round trip")
    val loss = env("BFF_LOSS_PERCENT", "0").toInt
    if loss > 0 then println(s"Simulated loss: $loss% of the datagrams")
    GameLink.open(access, lag, loss).onComplete {
      case Failure(error) =>
        println(s"Connection failed: ${error.getMessage}")
        backToMenus match {
          case Some(back) => back(GameExit.CannotConnect(error.getMessage))
          case None => Event.quit(1)
        }
      case Success(link) =>
        // otherwise, the error is only printed, and the screen stays as it is
        try play(link, access, headless, backToMenus)
        catch {
          case NonFatal(error) =>
            println(s"The game could not start: $error")
            error.printStackTrace()
            link.close()
            backToMenus match {
              case Some(back) => back(GameExit.CannotConnect(s"the game could not start ($error)"))
              case None => Event.quit(1)
            }
        }
    }
  }

  private def play(
      link: GameLink,
      access: GameAccess,
      headless: Boolean,
      backToMenus: Option[GameExit => Unit]
  ): Unit = {
    println("Connected")
    val (controls, controlsInfo) = ControlsFile.load()
    println(controlsInfo)
    if headless then {
      val client = new GameClient(
        link,
        access.userName,
        headless,
        controls,
        controlsInfo,
        () => Complex.zero,
        () => None
      )
      Love.update = dt => client.update(dt)
    } else {
      var renderer: Renderer = null
      val client = new GameClient(
        link,
        access.userName,
        headless,
        controls,
        controlsInfo,
        () => renderer.mouseWorld,
        () => renderer.hud.hovered
      )
      renderer = Renderer(client, ClientSettings.load().hudScale)
      // The dialogs over the game (game menu, end of game) are in the HUD's Gui, unscaled: they
      // take all the inputs while shown
      val gui = renderer.hud.gui
      val leave =
        backToMenus.map { back => () =>
          val exit = client.exit; client.leave(); back(exit)
        }
      val gameMenu  = GameMenu(gui.root, client, leave)
      val endOfGame = EndOfGame(gui.root, client, leave)
      gui.root.setScript(Script.OnUpdate) { _ =>
        gameMenu.update(); endOfGame.update()
      }
      Love.update = dt => client.update(dt)
      Love.draw = () => renderer.draw()
      Love.keypressed = (_, scancode, isRepeat) =>
        if scancode == Fullscreen.key then { if !isRepeat then Fullscreen.toggle() }
        else if !gui.keyPressed(scancode) && !isRepeat then client.keyPressed(scancode)
      Love.keyreleased = (_, scancode) =>
        if !client.finished && !client.menuOpen then client.keyReleased(scancode)
      // The frames that take the click (targets, "Start game", dialogs) get it, otherwise the left
      // button clicks the world (targets, zones), and the others can be bound in the controls
      Love.mousepressed = (x, y, button, _, _) =>
        if !gui.mousePressed(x, y, button) && button == 1 then
          client.clicked(renderer.toWorld(x, y))
      Love.mousereleased = (x, y, button, _, _) =>
        if !gui.mouseReleased(x, y, button) && button != 1 then client.mouseButtonReleased(button)
    }
  }
}
