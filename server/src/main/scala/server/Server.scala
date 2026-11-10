package server

import io.circe.parser.decode
import menus.data.User
import cask.main.Routes
import communication.udp.{JvmUDPSocket, Lobby}
import services.crypto.Crypto

object Server extends cask.MainRoutes with JsonEndpoints {

  given env: BackendEnv = BackendEnv.live()

  /** The UDP port of the rendezvous (see [[RendezvousServer]]), the same as the HTTP one by
    * default.
    */
  val rendezvousPort: Int =
    Option(java.lang.System.getProperty("rendezvousPort")).fold(port)(_.toInt)

  /** The address that the rendezvous' UDP socket binds to, all of them in prod, the loopback
    * otherwise (or the `rendezvousAddress` property). On fly.io, it must be `fly-global-services`:
    * bound to 0.0.0.0, Linux replies from the wrong source address, and the NATs of the players
    * drop the replies.
    */
  val rendezvousAddress: String =
    Option(java.lang.System.getProperty("rendezvousAddress")).filter(_.nonEmpty).getOrElse {
      if isProd then "0.0.0.0" else "127.0.0.1"
    }

  /** The game server's jar, that the players download to run it, relative to where the server runs
    * (or the `gameServerJar` property).
    */
  val gameServerJar: os.Path = Option(java.lang.System.getProperty("gameServerJar"))
    .fold(os.pwd / os.RelPath("..") / "game-server" / "target" / "game-server.jar")(
      os.Path(_, os.pwd)
    )

  /** The Java runtimes that the players run the game server with (`sbt
    * game-server/gameServerRuntimes`), relative to where the server runs (or the
    * `gameServerRuntimes` property). Without them, the players need Java.
    */
  val gameServerRuntimes: os.Path = Option(java.lang.System.getProperty("gameServerRuntimes"))
    .fold(os.pwd / os.RelPath("..") / "game-server" / "target" / "runtimes")(os.Path(_, os.pwd))

  /** The packages of the LÖVE client (`sbt love-client/luaPackage`), that the players can download
    * (see [[ClientPackageRoutes]]), relative to where the server runs (or the `clientPackages`
    * property).
    */
  val clientPackages: os.Path = Option(java.lang.System.getProperty("clientPackages"))
    .fold(os.pwd / os.RelPath("..") / "love-client" / "target" / "dist")(os.Path(_, os.pwd))

  /** Of the rendezvous and of the lobby, which share it (see `communication.udp.Lobby`). */
  private val udpSocket = JvmUDPSocket()

  val lobby: LobbyServer = LobbyServer(udpSocket, rendezvousPort, env.events)

  val rendezvous: RendezvousServer = RendezvousServer(
    Lobby.SharedSocket(udpSocket, lobby.received),
    gameId => env.menuGames.gameCredentials(gameId).map(_.allGameCredentials)
  )

  override def allRoutes: Seq[Routes] =
    Vector(
      this,
      MenuGameRoutes(rendezvousPort, lobby),
      GameLaunchingRoutes(rendezvous, rendezvousPort, gameServerJar, gameServerRuntimes),
      ClientPackageRoutes(clientPackages),
      LandingRoutes()
    )

  /** cask's, with the [[RequestLimits]] first. */
  override def defaultHandler: io.undertow.server.handlers.BlockingHandler = {
    val handler = super.defaultHandler
    handler.setRootHandler(RequestLimits(handler.getHandler))
    handler
  }

  override def port: Int = Option(java.lang.System.getProperty("port")).fold(9000)(_.toInt)

  private def isProd: Boolean =
    Option(java.lang.System.getProperty("isProd")).forall(_.toBoolean)

  /** Where the HTTP server listens: all the interfaces in prod, the loopback otherwise, or the
    * `httpAddress` property (the loopback behind a reverse proxy that does the TLS, see deploy.md).
    */
  override def host: String =
    Option(java.lang.System.getProperty("httpAddress")).filter(_.nonEmpty).getOrElse {
      if isProd then "0.0.0.0" else "127.0.0.1"
    }

  @getJ[Option[User]]("/api/users/me")
  def me(request: cask.Request) = request.cookies.get("session").map(session => User(session.value))

  @cask.get("/api/oupsy")
  def oupsy() =
    try throw new RuntimeException("bleh")
    catch { case e: RuntimeException => cask.Response(e.getMessage()) }

  @cask.post("/api/users/login")
  def login(request: cask.Request) = {
    val user = decode[User](request.text()).toTry.get
    cask.Response("", 200, Seq(), Seq(cask.Cookie("session", user.name, path = "/")))
  }

  @cask.get("/api")
  def hello() = "Hello World!"

  @cask.get("/api/ping/:name")
  def ping(name: String) = cask.Response("PONG: " ++ name)

  @cask.post("/api/do-thing")
  def doThing(request: cask.Request) = cask.Response(Crypto.hashPassword(request.text()).pw)

  initialize()

  rendezvous.start(rendezvousPort, Some(rendezvousAddress))

  println(
    s"Server listening at $host:$port (rendezvous on UDP $rendezvousAddress:$rendezvousPort)"
  )
}
