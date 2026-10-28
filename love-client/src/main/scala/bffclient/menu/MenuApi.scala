package bffclient.menu

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js

import bffclient.game.GameAccess
import bffclient.lua.{WebSocket, WebSocketModule}
import communication.udp.Peer
import io.circe.parser.decode
import menus.data.*
import models.bff.outofgame.MenuGameWithPlayers
import models.bff.outofgame.gameconfig.{GameConfiguration, PlayerInfo}

/** The endpoints of Battle for Flatland's menu server, like the frontend's `services.menugames`. */
final class MenuApi(val host: String, val port: Int)(using ExecutionContext) {
  private val http = new HttpClient(s"http://$host:$port")

  /** How the address is shown (and remembered). */
  def address: String = s"$host:$port"

  def sessionCookie: String = http.sessionCookie

  /** Logs in (the menu server only asks for a name) and returns the logged in user. */
  def login(name: String): Future[User] = for
    _    <- http.post("/api/users/login", User(name))
    me   <- me
    user <- me.fold(Future.failed(HttpError(s"Login as $name failed")))(Future.successful)
  yield user

  def me: Future[Option[User]] = http.get[Option[User]]("/api/users/me")

  def games: Future[Vector[MenuGameWithPlayers]] =
    http.get[Vector[MenuGameWithPlayers]]("/api/bff/games")

  def game(gameId: String): Future[Option[MenuGameWithPlayers]] = games.map(_.find(_.id == gameId))

  def createGame(name: String): Future[MenuGameWithPlayers] =
    http.postApi[CreateGameFormData, MenuGameWithPlayers](
      "/api/bff/new-game",
      CreateGameFormData(name)
    )

  def joinGame(gameId: String): Future[Vector[MenuGameWithPlayers]] =
    http.postApi[JoinGameFormData, Vector[MenuGameWithPlayers]](
      "/api/bff/join-game",
      JoinGameFormData(gameId, None)
    )

  def leaveGame(gameId: String): Future[MenuGameWithPlayers] =
    http.postApi[GameIdFormData, MenuGameWithPlayers]("/api/bff/leave-game", GameIdFormData(gameId))

  def kickPlayer(gameId: String, playerName: String): Future[MenuGameWithPlayers] =
    http.postApi[KickPlayerFormData, MenuGameWithPlayers](
      "/api/bff/kick-player",
      KickPlayerFormData(gameId, playerName)
    )

  def changePlayerInfo(gameId: String, info: PlayerInfo): Future[MenuGameWithPlayers] =
    http.postApi[ChangePlayerInfoFormData, MenuGameWithPlayers](
      "/api/bff/change-player-info",
      ChangePlayerInfoFormData(gameId, info)
    )

  def changeGameConfig(
      gameId: String,
      metadata: GameConfiguration.GameConfigMetadata
  ): Future[MenuGameWithPlayers] =
    http.postApi[ChangeGameMetadataFormData, MenuGameWithPlayers](
      "/api/bff/change-game-config",
      ChangeGameMetadataFormData(gameId, metadata)
    )

  def addAI(gameId: String): Future[Boolean] =
    http.postApi[GameIdFormData, Boolean]("/api/bff/add-ai-to-game", GameIdFormData(gameId))

  def startGame(gameId: String): Future[Boolean] =
    http.postApi[GameIdFormData, Boolean]("/api/bff/start-game", GameIdFormData(gameId))

  /** How to reach the game server, from what the lobby websocket says. */
  def gameAccess(
      userName: String,
      credentials: MenuGameComm.HereAreYourCredentials
  ): GameAccess =
    GameAccess(
      credentials.gameServerAddresses.map(address => Peer(address.host, address.port)),
      Some(Peer(host, credentials.rendezvousPort)),
      userName,
      credentials.gameId,
      credentials.secret
    )

  /** Our candidacy to run the game server, when asked for it (see [[MenuGameComm]]). */
  def hostCandidacy(candidacy: HostCandidacy): Future[Boolean] =
    http.postApi[HostCandidacy, Boolean]("/api/bff/host-candidacy", candidacy)

  /** We were chosen to run the game server, but we cannot: someone else will. */
  def hostFailed(gameId: String, reason: String): Future[Boolean] =
    http.postApi[HostFailedFormData, Boolean](
      "/api/bff/host-failed",
      HostFailedFormData(gameId, reason)
    )

  /** The version of the game server's jar: its SHA-256 and size. */
  def gameServerVersion: Future[GameServerFile] =
    http.get[GameServerFile]("/api/bff/game-server/version")

  /** Downloads the game server's jar to `file` (a path of the operating system), telling
    * `onProgress` the bytes received so far.
    */
  def downloadGameServer(file: String, onProgress: Double => Unit): Future[Unit] =
    http.download("/api/bff/game-server/jar", file, onProgress)

  /** The version of the Java runtime to run the game server with on this platform (e.g.
    * `linux-x64`), if the menu server has one: the SHA-256 of its manifest (`bff-runtime.sha256`),
    * and the size of its archive.
    */
  def gameServerRuntimeVersion(platform: String): Future[Option[GameServerFile]] =
    http.get[Option[GameServerFile]](s"/api/bff/game-server/runtime/version?platform=$platform")

  /** Downloads the Java runtime of this platform (a tar.gz) to `file` (a path of the operating
    * system), telling `onProgress` the bytes received so far.
    */
  def downloadGameServerRuntime(
      platform: String,
      file: String,
      onProgress: Double => Unit
  ): Future[Unit] =
    http.download(s"/api/bff/game-server/runtime?platform=$platform", file, onProgress)

  /** The lobby websocket: tells when data changed, chooses who runs the game server, and pushes the
    * game server credentials. With a game id, it must stay open (also during the game): the menu
    * server kicks the players who are not connected.
    */
  def lobby(gameId: Option[String])(onMessage: MenuGameComm => Unit): Future[Lobby] = {
    val query      = gameId.fold("")(id => s"?gameId=$id")
    val connection =
      WebSocketModule.connect(s"ws://$host:$port/ws/bff/game-anti-chamber$query", 5, sessionCookie)
    if connection.socket == null then Future.failed(HttpError(s"Lobby websocket: ${connection.error}"))
    else Future.successful(new Lobby(connection.socket, onMessage))
  }
}

object MenuApi {
  val defaultPort = 9000

  /** The host and port of an address typed by a player: `host`, `host:port`, possibly with
    * `http://` and a trailing slash. Without a port, the menu server's default one.
    */
  def parseAddress(address: String): Either[String, (String, Int)] = {
    val trimmed = address.trim.stripPrefix("http://").stripSuffix("/")
    if address.trim.startsWith("https://") then Left("https is not supported, use http://")
    else
      trimmed.split(":") match {
        case Array(host) if host.nonEmpty => Right((host, defaultPort))
        case Array(host, port) if host.nonEmpty =>
          port.toIntOption
            .filter(p => p > 0 && p < 65536)
            .map(p => (host, p))
            .toRight(s"Invalid port: $port")
        case _ => Left(s"Invalid address: $address (expected host or host:port)")
      }
  }
}

/** An open lobby websocket, polled every frame. */
final class Lobby(ws: WebSocket, onMessage: MenuGameComm => Unit) {
  private val poller = js.timers.setInterval(1)(poll())
  private var closed = false

  def isOpen: Boolean = !closed && ws.isOpen()

  def close(): Unit = if !closed then {
    closed = true
    js.timers.clearInterval(poller)
    ws.close()
  }

  private def poll(): Unit = {
    var data = ws.poll()
    while data != null && !closed do {
      decode[MenuGameComm](data) match {
        case Right(message) => onMessage(message)
        case Left(error) => println(s"[lobby] Unknown message $data: $error")
      }
      data = ws.poll()
    }
  }
}
