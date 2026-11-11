package server

import menus.data.APIResponse
import menus.data.{GameCredentialsWithGameInfo, GameServerAddress, GameServerFile}
import services.events.Event
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
)(using env: BackendEnv)
    extends cask.Routes
    with JsonEndpoints {

  @getJ[APIResponse[GameCredentialsWithGameInfo]]("api/bff/internal/get-all-credentials")
  def retrieveAllGameCredentials(gameId: Int, secret: String) = {
    println(s"Request to provide credentials for $gameId")
    APIResponse.fromEither(env.menuGames.retrieveAllGameCredentials(gameId, secret))
  }

  /** The game server is ready, on UDP `port`, and on `addresses` (comma separated IP addresses) of
    * its local network. Its public address is the one the rendezvous sees it from, if it registered
    * there (or the one it told the rendezvous, when that one sees it on a private network).
    */
  @getJ[APIResponse[Boolean]]("api/bff/internal/game-server-ready")
  def gameServerReady(
      ctx: cask.Request,
      gameId: Int,
      secret: String,
      port: Int,
      addresses: String = ""
  ) =
    APIResponse.fromEither(
      env.menuGames.retrieveAllGameCredentials(gameId, secret).map { gameInfo =>
        val public = rendezvous
          .gameServerAddresses(gameId)
          .map(peer => GameServerAddress(peer.address, peer.port))
        val local = addresses.split(",").toVector.filter(_.nonEmpty).map(GameServerAddress(_, port))
        println(s"game server ready for $gameId, seen at ${public.mkString(", ")}")
        val host = env.hostElections.over(gameId)
        env.events.dispatchEvent(
          Event.GameCredentials(
            gameInfo.allGameCredentials,
            (public ++ local).distinct,
            host,
            port,
            rendezvousPort
          )
        )
        true
      }
    )

  /** The version of the game server's jar (a [[GameServerFile]]): its SHA-256, so that the players
    * download it only when it changed, and check what they downloaded, and its size.
    */
  @cask.get("api/bff/game-server/version")
  def gameServerVersion(): cask.Response.Raw = GameLaunchingRoutes.file(gameServerJar) match {
    case Some(file) => json(Some(file))
    case None => noJar
  }

  @cask.get("api/bff/game-server/jar")
  def gameServerJarFile(request: cask.Request): cask.Response.Raw =
    if os.exists(gameServerJar) then
      GameLaunchingRoutes.fileResponse(gameServerJar, "application/java-archive", request)
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
  def gameServerRuntime(request: cask.Request, platform: String): cask.Response.Raw =
    runtimeFile(platform, "tar.gz").filter(os.exists) match {
      case Some(archive) => GameLaunchingRoutes.fileResponse(archive, "application/gzip", request)
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

  private val MaxRange = 16L << 20

  private val ByteRange = """bytes=(\d+)-(\d*)""".r

  /** The file, or the part of it that the `Range` header asks for (`bytes=<first>-[<last>]`, the
    * only form supported), which the clients use to download it a piece at a time (LÖVE's HTTPS
    * gives nothing before the end of a response, so that is how they tell the progress).
    */
  def fileResponse(file: os.Path, contentType: String, request: cask.Request): cask.Response.Raw = {
    val size = os.size(file)
    request.headers.get("range").flatMap(_.headOption) match {
      case Some(ByteRange(first, last)) if first.toLong < size =>
        val from = first.toLong
        // (in memory: not more than MaxRange at a time, which the clients ask for anyway)
        val until =
          math.min(last.toLongOption.fold(size)(l => math.min(l + 1, size)), from + MaxRange)
        val bytes = {
          val channel = java.nio.channels.FileChannel.open(file.toNIO)
          try {
            val buffer = java.nio.ByteBuffer.allocate((until - from).toInt)
            while buffer.hasRemaining && channel.read(buffer, from + buffer.position()) >= 0 do ()
            buffer.array()
          } finally channel.close()
        }
        cask.Response[cask.Response.Data](
          bytes,
          statusCode = 206,
          headers = Seq(
            "Content-Type"  -> contentType,
            "Content-Range" -> s"bytes $from-${until - 1}/$size"
          )
        )
      case Some(_) =>
        cask.Response[cask.Response.Data](
          "",
          statusCode = 416,
          headers = Seq("Content-Range" -> s"bytes */$size")
        )
      case None =>
        cask.Response[cask.Response.Data](
          os.read.stream(file),
          headers = Seq("Content-Type" -> contentType, "Accept-Ranges" -> "bytes")
        )
    }
  }

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
