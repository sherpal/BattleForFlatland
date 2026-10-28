package server

import ziocask.WithZIOEndpoints
import menus.data.APIResponse
import services.{events, hostelections, menugames}
import zio.*
import menus.data.{GameCredentialsWithGameInfo, GameServerAddress, GameServerFile}
import io.circe.syntax.*

/** What the game servers (run by one of the players of their game, see [[programs.launchGame]])
  * need from the menu server.
  *
  * @param gameServerJar
  *   the game server's fat jar (`sbt game-server/assembly`), that the players download to run it
  * @param gameServerRuntimes
  *   the Java runtimes that they run it with (`sbt game-server/gameServerRuntimes`): for each
  *   platform (e.g. `linux-x64`), `<platform>.tar.gz` and its manifest, `<platform>.sha256`
  */
class GameLaunchingRoutes(
    rendezvous: RendezvousServer,
    rendezvousPort: Int,
    gameServerJar: os.Path,
    gameServerRuntimes: os.Path
)(using
    val runtime: zio.Runtime[BackendEnv]
) extends cask.Routes
    with WithZIOEndpoints[BackendEnv] {

  @caskz.getJ[APIResponse[GameCredentialsWithGameInfo]]("api/bff/internal/get-all-credentials")
  def retrieveAllGameCredentials(gameId: String, secret: String) = (for {
    _           <- Console.printLine(s"Request to provide credentials for $gameId").ignore
    credentials <- menugames.retrieveAllGameCredentials(gameId, secret)
  } yield credentials).map(APIResponse.fromEither)

  /** The game server is ready, on UDP `port`, and on `addresses` (comma separated IP addresses) of
    * its local network. Its public address is the one the rendezvous sees it from, if it registered
    * there.
    */
  @caskz.getJ[APIResponse[Boolean]]("api/bff/internal/game-server-ready")
  def gameServerReady(
      ctx: cask.Request,
      gameId: String,
      secret: String,
      port: Int,
      addresses: String = ""
  ) = (for {
    gameInfo <- menugames.retrieveAllGameCredentials(gameId, secret).flatMap(ZIO.fromEither)
    public = rendezvous
      .gameServerAddress(gameId)
      .map(peer => GameServerAddress(peer.address, peer.port))
    local = addresses.split(",").toVector.filter(_.nonEmpty).map(GameServerAddress(_, port))
    _    <- Console.printLine(s"game server ready for $gameId, public address: $public").orDie
    host <- hostelections.over(gameId)
    _ <- events.dispatchEvent(
      events.Event.GameCredentials(
        gameInfo.allGameCredentials,
        (public.toVector ++ local).distinct,
        host,
        port,
        rendezvousPort
      )
    )
  } yield true).either.map(APIResponse.fromEither)

  /** The version of the game server's jar (a [[GameServerFile]]): its SHA-256, so that the players
    * download it only when it changed, and check what they downloaded, and its size.
    */
  @cask.get("api/bff/game-server/version")
  def gameServerVersion(): cask.Response.Raw = GameLaunchingRoutes.file(gameServerJar) match {
    case Some(file) => json(Some(file))
    case None => noJar
  }

  @cask.get("api/bff/game-server/jar")
  def gameServerJarFile(): cask.Response.Raw =
    if os.exists(gameServerJar) then
      cask.Response[cask.Response.Data](
        os.read.stream(gameServerJar),
        headers = Seq("Content-Type" -> "application/java-archive")
      )
    else noJar

  /** The version of the Java runtime of this platform (e.g. `linux-x64`), a [[GameServerFile]]: the
    * SHA-256 of its manifest, which has the SHA-256 of each of its files, and the size of its
    * archive. `null` when there is none (the player then needs Java).
    */
  @cask.get("api/bff/game-server/runtime/version")
  def gameServerRuntimeVersion(platform: String): cask.Response.Raw =
    json(for
      manifest <- runtimeFile(platform, "sha256")
      archive  <- runtimeFile(platform, "tar.gz").filter(os.exists)
      version  <- GameLaunchingRoutes.file(manifest)
    yield version.copy(size = os.size(archive)))

  /** The Java runtime of this platform, a tar.gz (with its manifest, `bff-runtime.sha256`). */
  @cask.get("api/bff/game-server/runtime")
  def gameServerRuntime(platform: String): cask.Response.Raw =
    runtimeFile(platform, "tar.gz").filter(os.exists) match {
      case Some(archive) =>
        cask.Response[cask.Response.Data](
          os.read.stream(archive),
          headers = Seq("Content-Type" -> "application/gzip")
        )
      case None => cask.Response[cask.Response.Data](s"No Java runtime for $platform", 404)
    }

  private def runtimeFile(platform: String, extension: String): Option[os.Path] =
    Option.when(platform.matches("[a-z0-9]+-[a-z0-9]+"))(
      gameServerRuntimes / s"$platform.$extension"
    )

  private def json(file: Option[GameServerFile]): cask.Response.Raw =
    cask.Response[cask.Response.Data](
      file.asJson.noSpaces,
      headers = Seq("Content-Type" -> "application/json")
    )

  private def noJar = {
    val message = s"No game server at $gameServerJar (run `sbt game-server/assembly`)"
    java.lang.System.err.println(message)
    cask.Response[cask.Response.Data](message, statusCode = 404)
  }

  initialize()
}

object GameLaunchingRoutes {

  // by file: (last modified, sha-256), computed again when the file changes
  private var cachedSha256 = Map.empty[os.Path, (Long, String)]

  /** The SHA-256 of the file (in hexadecimal) and its size, if it exists. */
  private def file(file: os.Path): Option[GameServerFile] =
    sha256(file).map(GameServerFile(_, os.size(file)))

  /** The SHA-256 of the file (in hexadecimal), if it exists. */
  private def sha256(file: os.Path): Option[String] = synchronized {
    Option.when(os.exists(file)) {
      val modified = os.mtime(file)
      cachedSha256.get(file).filter(_._1 == modified).map(_._2).getOrElse {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val stream = os.read.inputStream(file)
        try {
          val buffer = new Array[Byte](1 << 16)
          var read   = stream.read(buffer)
          while read >= 0 do {
            digest.update(buffer, 0, read)
            read = stream.read(buffer)
          }
        } finally stream.close()
        val sha256 = digest.digest().map("%02x".format(_)).mkString
        cachedSha256 += file -> (modified, sha256)
        sha256
      }
    }
  }
}
