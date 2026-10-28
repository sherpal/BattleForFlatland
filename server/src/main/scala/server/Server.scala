package server

import io.circe.parser.decode
import cask.model.Request
import cask.router.Result
import zio.*
import menus.data.User
import cask.main.Routes
import communication.udp.JvmUDPSocket

object Server extends cask.MainRoutes with ziocask.WithZIOEndpoints[BackendEnv] {

  val layer = ZLayer.make[BackendEnv](
    services.crypto.Crypto.live,
    services.menugames.MenuGames.live,
    services.localstorage.BLocalStorage.live,
    services.events.Events.live,
    services.hostelections.HostElections.live
  )

  val runtime: Runtime[BackendEnv] =
    Unsafe.unsafe { implicit unsafe =>
      Runtime.unsafe.fromLayer(layer)
    }

  /** The UDP port of the rendezvous (see [[RendezvousServer]]), the same as the HTTP one by
    * default.
    */
  val rendezvousPort: Int =
    Option(java.lang.System.getProperty("rendezvousPort")).fold(port)(_.toInt)

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

  val rendezvous: RendezvousServer = RendezvousServer(
    JvmUDPSocket(),
    gameId =>
      Unsafe.unsafe { implicit unsafe =>
        runtime.unsafe
          .run(services.menugames.gameCredentials(gameId))
          .getOrThrow()
          .map(_.allGameCredentials)
      }
  )

  override def allRoutes: Seq[Routes] = {
    given Runtime[BackendEnv] = runtime
    Vector(
      this,
      MenuGameRoutes(rendezvousPort),
      GameLaunchingRoutes(rendezvous, rendezvousPort, gameServerJar, gameServerRuntimes),
      ClientPackageRoutes(clientPackages)
    )
  }

  override def port: Int = Option(java.lang.System.getProperty("port")).fold(9000)(_.toInt)

  override def host: String = Option(java.lang.System.getProperty("isProd"))
    .map(_.toBoolean)
    .fold("0.0.0.0")(isProd => if isProd then "0.0.0.0" else "127.0.0.1")

  @caskz.getJ[Option[User]]("/api/users/me")
  def me(request: cask.Request) =
    ZIO.succeed(request.cookies.get("session").map(session => User(session.value)))

  @caskz.get[String]("/api/oupsy")
  def oupsy() =
    zio.ZIO
      .succeed(throw new RuntimeException("bleh"))
      .catchAllCause(cause => zio.ZIO.succeed(cask.Response(cause.squashTrace.getMessage())))

  @caskz.post[String]("/api/users/login")
  def login(request: cask.Request) = for {
    user <- ZIO.fromEither(decode[User](request.text()))
  } yield cask.Response("", 200, Seq(), Seq(cask.Cookie("session", user.name, path = "/")))

  @cask.get("/api")
  def hello() = "Hello World!"

  @caskz.get[String]("/api/ping/:name")
  def ping(name: String) = ZIO.succeed(cask.Response("PONG: " ++ name))

  @caskz.post[String]("/api/do-thing")
  def doThing(request: cask.Request) =
    for {
      body        <- ZIO.succeed(request.text())
      encodedBody <- services.crypto.hashPassword(body)
    } yield cask.Response(encodedBody.pw)

  initialize()

  rendezvous.start(rendezvousPort, Some(host))

  println(s"Server listening at $host:$port (rendezvous on UDP $rendezvousPort)")
}
