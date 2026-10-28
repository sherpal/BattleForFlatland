package server

/** The packages of the LÖVE client (`sbt love-client/luaPackage`), to download them from the menu
  * server: `api/bff/client/<system>`, with `<system>` one of `windows`, `macos`, `linux` (the
  * archives), or `love` (the game alone, for those who have LÖVE).
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
