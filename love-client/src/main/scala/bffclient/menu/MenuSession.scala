package bffclient.menu

import scala.concurrent.{ExecutionContext, Future, Promise}

import bffclient.game.GameAccess
import menus.data.*
import models.bff.outofgame.PlayerClasses
import models.bff.outofgame.gameconfig.*
import utils.misc.RGBColour

/** Plays the menus against the menu server like the frontend does, without a UI (for headless
  * runs): logs in, creates a game, chooses a class and gets ready, picks the boss, starts the game
  * and waits for the game server credentials (pushed on the lobby websocket).
  */
final class MenuSession(
    api: MenuApi,
    userName: String,
    playerClass: PlayerClasses,
    bossName: String
)(using ExecutionContext) {

  private val credentials = Promise[GameAccess]()

  def info(message: String): Unit = println(s"[menu] $message")

  def run(): Future[GameAccess] = for
    _  <- Future.successful(info(s"Logging in as $userName"))
    me <- api.login(userName)
    _ = info(s"Logged in: $me (cookie ${api.sessionCookie})")
    games <- api.games
    _ = info(s"${games.size} game(s) on the server")
    game <- api.createGame(s"ScalaLove ${System.currentTimeMillis()}")
    _ = info(s"Created game '${game.game.gameName}' (${game.id})")
    _ <- api.lobby(Some(game.id)) {
      case MenuGameComm.HereAreYourCredentials(id, secret, port) =>
        credentials.trySuccess(GameAccess(api.host, port, userName, id, secret))
      case MenuGameComm.GameStarted() => info("<- GameStarted")
      case MenuGameComm.DataUpdated() => ()
    }
    _ = info("Lobby websocket connected")
    _ <- api.changePlayerInfo(game.id, readyPlayerInfo)
    _ = info(s"Playing a $playerClass, ready")
    config <- api.changeGameConfig(game.id, GameConfiguration.GameConfigMetadata(bossName))
    _ = info(
      s"Boss: ${config.game.gameConfiguration.bossName}, configuration valid: ${config.game.gameConfigurationIsValid}"
    )
    started <- api.startGame(game.id)
    _ = info(s"Start game: $started, waiting for the game server...")
    access <- credentials.future
    _ = info(s"Game server ready on port ${access.port}")
  yield access

  private def readyPlayerInfo = PlayerInfo(
    PlayerName.HumanPlayerName(userName),
    Some(playerClass),
    Some(RGBColour(220, 40, 40)),
    PlayerStatus.Ready,
    PlayerType.Human
  )
}
