package server

import io.circe.syntax.*

/** The packages of the LÖVE client (`sbt love-client/luaPackage`), to download them from the menu
  * server: `api/bff/client/<system>`, with `<system>` one of `windows`, `macos`, `linux` (the
  * archives), or `love` (the game alone, for those who have LÖVE). The [[LandingPage]] links to
  * them. Their version is `api/bff/client-version`.
  *
  * @param clientPackages
  *   where `luaPackage` puts them (`love-client/target/dist`)
  */
class ClientPackageRoutes(clientPackages: os.Path) extends cask.Routes {
  import ClientPackageRoutes.*

  @cask.get("api/bff/client/:system")
  def clientPackage(system: String): cask.Response.Raw = packages.get(system) match {
    case None =>
      cask.Response[cask.Response.Data](
        s"No package for $system (one of ${packages.keys.mkString(", ")})",
        statusCode = 404
      )
    case Some((fileName, _)) if !os.exists(clientPackages / fileName) =>
      val message =
        s"No package at ${clientPackages / fileName} (run `sbt love-client/luaPackage`)"
      java.lang.System.err.println(message)
      cask.Response[cask.Response.Data](message, statusCode = 404)
    case Some((fileName, contentType)) =>
      val file = clientPackages / fileName
      cask.Response[cask.Response.Data](
        os.read.stream(file),
        headers = Seq(
          "Content-Type"        -> contentType,
          "Content-Length"      -> os.size(file).toString,
          "Content-Disposition" -> s"attachment; filename=\"$fileName\""
        )
      )
  }

  /** The version of the packages (the `version` file that `luaPackage` writes next to them), or
    * `null` without one. The packaged clients compare it to theirs: when it differs, the player
    * must download the game again.
    */
  @cask.get("api/bff/client-version")
  def clientVersion(): cask.Response.Raw = {
    val file    = clientPackages / "version"
    val version = Option.when(os.exists(file))(os.read(file).trim).filter(_.nonEmpty)
    cask.Response[cask.Response.Data](
      version.asJson.noSpaces,
      headers = Seq("Content-Type" -> "application/json")
    )
  }

  initialize()
}

object ClientPackageRoutes {

  /** By system: the file name, and its content type. */
  private val packages = Map(
    "windows" -> ("BattleForFlatland-windows.zip", "application/zip"),
    "macos"   -> ("BattleForFlatland-macos.zip", "application/zip"),
    "linux"   -> ("BattleForFlatland-linux.tar.gz", "application/gzip"),
    "love"    -> ("BattleForFlatland.love", "application/zip")
  )
}
