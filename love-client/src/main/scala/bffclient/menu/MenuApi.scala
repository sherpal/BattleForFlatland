package bffclient.menu

import scala.concurrent.{ExecutionContext, Future}

import bffclient.game.GameAccess
import bffclient.lua.BuildConfig
import communication.udp.Peer
import menus.data.*
import models.bff.outofgame.MenuGameWithPlayers
import models.bff.outofgame.gameconfig.{GameConfiguration, PlayerInfo}
import services.i18n.I18n

/** The endpoints of Battle for Flatland's menu server, like the frontend's `services.menugames`.
  *
  * @param scheme
  *   `https` (in front of the packaged clients' menu server, see deploy.md) or `http` (locally, or
  *   when a player runs it at home)
  */
final class MenuApi(val scheme: String, val host: String, val port: Int)(using ExecutionContext) {

  /** Where the menu server is, e.g. `https://bff.example.com` (also given to the game server). */
  val url: String =
    if port == MenuApi.standardPort(scheme) then s"$scheme://$host" else s"$scheme://$host:$port"

  private val http = new HttpClient(url)

  /** How the address is shown (and remembered): without the scheme and the port when they are the
    * default ones.
    */
  def address: String = {
    val hostAndPort = if port == MenuApi.defaultPort(scheme) then host else s"$host:$port"
    if scheme == MenuApi.defaultScheme then hostAndPort else s"$scheme://$hostAndPort"
  }

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

  /** How to reach the game server, from what the lobby says. */
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

  /** The version of the packaged clients that the menu server serves, if it has them (see
    * `BuildConfig.version`).
    */
  def clientVersion: Future[Option[String]] = http.get[Option[String]]("/api/bff/client-version")

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

  /** Subscribes to the messages of the menu server, of the list of games or of the lobby of a game
    * (see [[Lobby]]).
    */
  def subscribe(gameId: Option[String]): Future[LobbySubscription] =
    http.postApi[LobbySubscribeFormData, LobbySubscription](
      "/api/bff/lobby/subscribe",
      LobbySubscribeFormData(gameId)
    )

  /** The messages of the subscription after the `after`-th. */
  def lobbyMessages(subscription: String, after: Long): Future[Vector[LobbyMessage]] =
    http.getApi[Vector[LobbyMessage]](s"/api/bff/lobby/messages?id=$subscription&after=$after")

  def unsubscribe(subscription: String): Future[Boolean] =
    http.postApi[LobbyUnsubscribeFormData, Boolean](
      "/api/bff/lobby/unsubscribe",
      LobbyUnsubscribeFormData(subscription)
    )

  /** The messages of the menu server: when data changed, who runs the game server, its credentials.
    * With a game id, it must stay open (also during the game): the menu server kicks the players
    * who are not there.
    */
  def lobby(gameId: Option[String])(onMessage: MenuGameComm => Unit): Future[Lobby] =
    Lobby.open(this, gameId, onMessage)
}

object MenuApi {

  /** The scheme of an address typed without one: `https` for the packaged clients (their menu
    * server is behind TLS), `http` otherwise (the build's `menuServerScheme`).
    */
  def defaultScheme: String = BuildConfig.menuServerScheme.getOrElse("http")

  /** HTTPS on its usual port (behind a reverse proxy), HTTP on the menu server's. */
  def defaultPort(scheme: String): Int = if scheme == "https" then 443 else 9000

  /** The port of a URL without one. */
  def standardPort(scheme: String): Int = if scheme == "https" then 443 else 80

  /** The scheme, host and port of an address typed by a player: `host`, `host:port`, possibly with
    * `http://` or `https://` (otherwise [[defaultScheme]]) and a trailing slash. Without a port,
    * the default one of the scheme. What is wrong with it is told in the language of `i18n`.
    */
  def parseAddress(address: String, i18n: I18n): Either[String, (String, String, Int)] = {
    val trimmed = address.trim.stripSuffix("/")
    val (scheme, rest) = trimmed match {
      case s"http://$rest" => ("http", rest)
      case s"https://$rest" => ("https", rest)
      case rest => (defaultScheme, rest)
    }
    rest.split(":") match {
      case Array(host) if host.nonEmpty => Right((scheme, host, defaultPort(scheme)))
      case Array(host, port) if host.nonEmpty =>
        port.toIntOption
          .filter(p => p > 0 && p < 65536)
          .map(p => (scheme, host, p))
          .toRight(i18n.invalidPort(port))
      case _ => Left(i18n.invalidAddress(address))
    }
  }
}
