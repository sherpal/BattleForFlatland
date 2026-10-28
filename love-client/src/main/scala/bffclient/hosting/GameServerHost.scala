package bffclient.hosting

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

import bffclient.love.{Filesystem, LoveSystem, Timer}
import bffclient.lua.{Jit, Os, ScalaLove}
import bffclient.menu.MenuApi
import menus.data.{GameServerFile, HostCandidacy}

/** Running the game server on this computer, when the menu server chooses us to (see
  * `menus.data.MenuGameComm`).
  *
  * The game server is the jar of the menu server (`api/bff/game-server/jar`), run with the Java
  * runtime that the menu server has for our platform (`api/bff/game-server/runtime`, a tar.gz unpacked
  * with `tar`), or, if it has none, with the `java` of the computer (17 or later). Both are
  * downloaded once per version into the save directory (`game-servers/`), and checked, against the
  * SHA-256 that the menu server gives as their version (for the runtime, the one of its manifest,
  * which has the SHA-256 of each of its files): when the files were changed, or corrupted, they are
  * downloaded again. What is being downloaded is told by [[downloading]].
  *
  * BFF_JAVA runs the game server with that `java` (instead of the runtime, or of the one of the
  * PATH), and BFF_GAME_SERVER_JAR runs that jar (e.g. when developing).
  */
object GameServerHost {
  private val windows = LoveSystem.getOS() == "Windows"

  private val javaOverride = Option(Os.getenv("BFF_JAVA"))

  /** The `java` of the computer. */
  private val systemJava = javaOverride.getOrElse("java")

  /** Our platform, as the menu server names its Java runtimes (e.g. `linux-x64`), if we know it. */
  private val platform: Option[String] = (LoveSystem.getOS() match {
    case "Linux" => Some("linux")
    case "OS X" => Some("macos")
    case "Windows" => Some("windows")
    case _ => None
  }).map(os => s"$os-${Jit.arch}")

  /** Name of the manifest, in the Java runtimes (see `project/JavaRuntimes.scala`). */
  private val runtimeManifest = "bff-runtime.sha256"

  /** Quoted for the system's shell. */
  private def quote(argument: String): String =
    if windows then "\"" + argument.replace("\"", "") + "\""
    else "'" + argument.replace("'", "'\\''") + "'"

  private val directoryName = "game-servers"
  private def directory     = s"${Filesystem.getSaveDirectory()}/$directoryName"

  /** What runs the game server: `java -jar jar`. */
  final case class GameServer(java: String, jar: String)

  /** The download of `what` (e.g. "the Java runtime"): `received` bytes out of `size`. */
  final case class Download(what: String, received: Double, size: Long) {
    def fraction: Double = if size <= 0 then 0 else math.min(1, received / size.toDouble)
  }

  private var currentDownload = Option.empty[Download]

  /** What is being downloaded (one thing at a time), if anything. */
  def downloading: Option[Download] = currentDownload

  /** Runs the download of `what` (given the function to tell the bytes received so far), told by
    * [[downloading]] meanwhile, and logged every quarter.
    */
  private def download(what: String, size: Long)(
      run: (Double => Unit) => Future[Unit]
  )(using ExecutionContext): Future[Unit] = {
    var loggedQuarters = 0
    currentDownload = Some(Download(what, 0, size))
    run { received =>
      val download = Download(what, received, size)
      currentDownload = Some(download)
      val quarters = (download.fraction * 4).toInt
      if quarters > loggedQuarters && quarters < 4 then {
        loggedQuarters = quarters
        println(s"[host] Downloading $what: ${quarters * 25}%")
      }
    }.andThen(_ => currentDownload = None)
  }

  private var probedJavaVersion = Option.empty[Future[Option[Int]]]

  /** The major version of the `java` of the computer, if it can be run. It is probed once (it takes
    * a while).
    */
  def javaVersion(using ExecutionContext): Future[Option[Int]] =
    probedJavaVersion.getOrElse {
      val probed = Processes.run(s"${quote(systemJava)} -version").map { output =>
        val version = parseJavaVersion(output)
        println(s"[host] Java of the computer: ${version.fold("none")(_.toString)}")
        version
      }
      probedJavaVersion = Some(probed)
      probed
    }

  /** The major version in what `java -version` says, e.g. `openjdk version "17.0.14"` (17), or
    * `java version "1.8.0_292"` (8).
    */
  def parseJavaVersion(output: String): Option[Int] = {
    val start = output.indexOf("version \"")
    Option
      .when(start >= 0) {
        val numbers = output.drop(start + 9).takeWhile(c => c.isDigit || c == '.').split('.')
        numbers.headOption.flatMap(_.toIntOption) match {
          case Some(1) => numbers.lift(1).flatMap(_.toIntOption)
          case major => major
        }
      }
      .flatten
  }

  /** @param ready
    *   whether we have what runs the game server already (see [[prepare]])
    */
  def candidacy(api: MenuApi, gameId: String, ready: Boolean)(using
      ExecutionContext
  ): Future[HostCandidacy] = {
    val start = Timer.getTime()
    for
      _ <- api.me
      roundTrip = ((Timer.getTime() - start) * 1000).toInt
      java    <- javaVersion
      runtime <- runtimeVersion(api).recover { case _ => None }
    yield HostCandidacy(
      gameId,
      java,
      runtime.isDefined,
      ready,
      LoveSystem.getProcessorCount(),
      LoveSystem.getOS(),
      roundTrip
    )
  }

  /** What runs the game server, once it is there: it is downloaded if needed (it takes a while, it
    * is better done before we are chosen to host the game). It fails when we cannot run it.
    */
  def prepare(api: MenuApi)(using ExecutionContext): Future[GameServer] = for
    java <- this.java(api)
    // only once we know that we can run it
    jar <- gameServerJar(api)
  yield GameServer(java, jar)

  /** Runs the game server, in the background (it outlives us). Completes when it was started, not
    * when it is ready (it tells the menu server itself).
    */
  def spawn(
      api: MenuApi,
      gameServer: Future[GameServer],
      gameId: String,
      gameSecret: String,
      rendezvousPort: Int
  )(using ExecutionContext): Future[Unit] = for
    server <- gameServer
    log       = s"$directory/game-server.log"
    arguments = Vector(api.host, api.port.toString, rendezvousPort.toString, gameId, gameSecret)
    javaCommand = (Vector(server.java, "-jar", server.jar) ++ arguments)
      .map(quote)
      .mkString(" ") + s" > ${quote(log)} 2>&1"
    command =
      if windows then s"start \"\" /B $javaCommand"
      else s"nohup $javaCommand < /dev/null &"
    _ = println(s"[host] Running the game server (${server.java} -jar ${server.jar}), into $log")
    output <- Processes.run(command)
  yield if output.trim.nonEmpty then println(s"[host] $output")

  /** The version of the Java runtime of the menu server for our platform, if it has one (and if
    * BFF_JAVA does not say which `java` to use).
    */
  private def runtimeVersion(api: MenuApi): Future[Option[GameServerFile]] =
    (javaOverride, platform) match {
      case (None, Some(platform)) => api.gameServerRuntimeVersion(platform)
      case _ => Future.successful(None)
    }

  /** The `java` to run the game server with: the one of our Java runtime (once it is there), or the
    * one of the computer if there is no runtime for us (or if ours failed), as long as it is recent
    * enough.
    */
  private def java(api: MenuApi)(using ExecutionContext): Future[String] =
    runtime(api).transformWith {
      case Success(Some(java)) => Future.successful(java)
      case outcome =>
        val runtimeFailure = outcome.failed.toOption.map(_.getMessage)
        runtimeFailure.foreach(failure => println(s"[host] Our Java runtime failed: $failure"))
        javaVersion.flatMap { version =>
          if version.exists(_ >= HostCandidacy.minimumJavaVersion) then
            Future.successful(systemJava)
          else
            Future.failed(
              Exception(
                runtimeFailure.getOrElse(
                  s"no Java ${HostCandidacy.minimumJavaVersion} or later on this computer"
                )
              )
            )
        }
    }

  /** The `java` of our Java runtime, once it is there and checked; none if there is no runtime for
    * us.
    */
  private def runtime(api: MenuApi)(using ExecutionContext): Future[Option[String]] =
    runtimeVersion(api).map(version => (platform, version)).flatMap {
      case (Some(platform), Some(GameServerFile(version, size))) =>
        val name     = s"java-$platform-${version.take(16)}"
        val java     = s"$directory/$name/bin/${if windows then "java.exe" else "java"}"
        def install(): Future[String] = {
          val archive = s"$directory/$name.tar.gz"
          removeAll("java-")
          Filesystem.createDirectory(s"$directoryName/$name")
          println(s"[host] Downloading the Java runtime ($name)")
          for
            _ <- download("the Java runtime", size)(
              api.downloadGameServerRuntime(platform, archive, _)
            )
            output <- Processes.run(
              s"tar -xzf ${quote(archive)} -C ${quote(s"$directory/$name")}"
            )
            _ = Filesystem.remove(s"$directoryName/$name.tar.gz")
            valid <- runtimeIsValid(name, version)
          yield
            if valid then {
              println(s"[host] Java runtime installed ($name)")
              java
            } else {
              removeAll(name)
              val tar = Option(output.trim).filter(_.nonEmpty).fold("")(output => s" (tar: $output)")
              throw Exception(s"the Java runtime is corrupted once unpacked$tar")
            }
        }
        once(name) {
          (if exists(name) then runtimeIsValid(name, version) else Future.successful(false))
            .flatMap(valid => if valid then Future.successful(java) else install())
        }.map(Some(_))
      case _ => Future.successful(None)
    }

  /** Whether the files of the runtime are the ones of its manifest, and the manifest the one of
    * this version.
    */
  private def runtimeIsValid(name: String, version: String)(using
      ExecutionContext
  ): Future[Boolean] =
    Digests
      .run(s"$directory/$name/$runtimeManifest")
      .flatMap { sha256 =>
        if sha256 != version then Future.successful(false)
        else {
          // lines of `sha256sum`: the SHA-256, two spaces, the path
          val entries = Filesystem
            .read(s"$directoryName/$name/$runtimeManifest")
            .contents
            .split('\n')
            .toVector
            .filter(_.nonEmpty)
            .map(line => (line.take(64), line.drop(66)))
          Future
            .traverse(entries)((expected, path) =>
              Digests.run(s"$directory/$name/$path").map(_ == expected)
            )
            .map(_.forall(identity))
        }
      }
      .recover { case _ => false } // a file is missing

  /** The path of the jar of the menu server's game server, once it is there and checked. */
  private def gameServerJar(api: MenuApi)(using ExecutionContext): Future[String] =
    Option(Os.getenv("BFF_GAME_SERVER_JAR")) match {
      case Some(jar) => Future.successful(jar)
      case None =>
        api.gameServerVersion.flatMap { case GameServerFile(version, size) =>
          val name = s"game-server-${version.take(16)}.jar"
          val path = s"$directory/$name"
          def isValid: Future[Boolean] = Digests.run(path).map(_ == version)
          def install(): Future[String] = {
            removeAll("game-server-")
            println(s"[host] Downloading the game server ($name)")
            for
              _     <- download("the game server", size)(api.downloadGameServer(path, _))
              valid <- isValid
            yield
              if valid then {
                println(s"[host] Game server downloaded ($name)")
                path
              } else {
                removeAll(name)
                throw Exception("the game server downloaded is corrupted")
              }
          }
          once(name) {
            (if exists(name) then isValid else Future.successful(false))
              .flatMap(valid => if valid then Future.successful(path) else install())
          }
        }
    }

  /** What is being installed or was, by name (so that what is being downloaded, e.g. by
    * [[prepare]], is not downloaded twice). The failures are forgotten, to be tried again. What
    * was installed is checked again on the next run of the client.
    */
  private var installs = Map.empty[String, Future[String]]

  private def once(name: String)(install: => Future[String])(using
      ExecutionContext
  ): Future[String] =
    installs.getOrElse(
      name, {
        Filesystem.createDirectory(directoryName)
        val installing = install
        installs += name -> installing
        installing.failed.foreach(_ => installs -= name)
        installing
      }
    )

  private def exists(name: String): Boolean = Filesystem.getInfo(s"$directoryName/$name") != null

  /** Removes what starts with this prefix in `game-servers/` (previous versions, or what failed),
    * directories with their content. What is used (by a game server still running, on Windows)
    * stays.
    */
  private def removeAll(prefix: String): Unit = {
    def remove(path: String): Unit = {
      if Filesystem.getInfo(path).kind == "directory" then
        ScalaLove
          .fromLuaSequence(Filesystem.getDirectoryItems(path))
          .foreach(item => remove(s"$path/$item"))
      Filesystem.remove(path)
    }
    ScalaLove
      .fromLuaSequence(Filesystem.getDirectoryItems(directoryName))
      .filter(_.startsWith(prefix))
      .foreach(item => remove(s"$directoryName/$item"))
  }
}
