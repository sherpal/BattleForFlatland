package bffclient

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

import bffclient.controls.{ClientSettings, ControlsFile}
import bffclient.game.{GameAccess, GameClient, GameExit, GameLink}
import bffclient.hud.{EndOfGame, GameMenu}
import bffclient.love.{Event, Fullscreen, Love}
import bffclient.lua.Os
import bffclient.menu.{MenuApi, MenuApp, MenuSession}
import bffclient.render.Renderer
import gamelogic.physics.Complex
import models.bff.outofgame.PlayerClasses
import bffclient.ui.Ui
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
        val (host, port) =
          MenuApi.parseAddress(url).fold(message => sys.error(s"BFF_MENU_URL: $message"), identity)
        val api         = new MenuApi(host, port)
        val playerClass = PlayerClasses
          .playerClassByName(env("BFF_CLASS", "Triangle"))
          .getOrElse(PlayerClasses.Triangle)
        val menu = new MenuSession(api, user, playerClass, env("BFF_BOSS", "Boss 101"))
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

  /** Connects to the game server (through UDP) and runs the game. When the game was joined through the
    * menus, `backToMenus` goes back to them.
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
      case Success(link) => play(link, access, headless, backToMenus)
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
      val client             = new GameClient(
        link,
        access.userName,
        headless,
        controls,
        controlsInfo,
        () => renderer.mouseWorld,
        () => renderer.hud.hovered
      )
      renderer = Renderer(client, ClientSettings.load().hudScale)
      // While an overlay is shown (game menu, end of game), the inputs go to it
      val ui    = new Ui
      val leave =
        backToMenus.map { back => () =>
          val exit = client.exit; client.leave(); back(exit)
        }
      val gameMenu     = GameMenu(client, leave)
      val endOfGame    = EndOfGame(client, leave)
      def overlayShown = client.finished || client.menuOpen
      Love.update = dt => client.update(dt)
      Love.draw = () => {
        renderer.draw()
        if client.finished then ui.frame(endOfGame.draw(ui))
        else if client.menuOpen then ui.frame(gameMenu.draw(ui))
      }
      Love.keypressed = (_, scancode, isRepeat) =>
        if scancode == Fullscreen.key then { if !isRepeat then Fullscreen.toggle() }
        else if overlayShown then ui.keyPressed(scancode)
        else if !isRepeat then client.keyPressed(scancode)
      Love.keyreleased = (_, scancode) => if !overlayShown then client.keyReleased(scancode)
      Love.mousepressed = (x, y, button, _, _) =>
        if overlayShown then ui.mousePressed(x, y, button)
        else if button == 1 then
          if renderer.hud.startButtonAt(x, y) then client.startCountdown()
          else
            renderer.hud.targetAt(x, y) match {
              case Some(id) => client.selectTarget(id) // a frame of the HUD
              case None => client.clicked(renderer.toWorld(x, y))
            }
      Love.mousereleased = (x, y, button, _, _) =>
        if overlayShown then ui.mouseReleased(x, y, button)
        else if button != 1 then
          client.mouseButtonReleased(button) // the left one targets and places zones
    }
  }
}
