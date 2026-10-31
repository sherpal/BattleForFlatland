package server

import scalatags.Text.all.*

/** The packages of the LÖVE client (`sbt love-client/luaPackage`), to download them from the menu
  * server: `api/bff/client/<system>`, with `<system>` one of `windows`, `macos`, `linux` (the
  * archives), or `love` (the game alone, for those who have LÖVE). The root route is a page that
  * links to them.
  *
  * @param clientPackages
  *   where `luaPackage` puts them (`love-client/target/dist`)
  */
class ClientPackageRoutes(clientPackages: os.Path) extends cask.Routes {
  import ClientPackageRoutes.*

  @cask.get("/")
  def downloadPage(): cask.Response.Raw =
    cask.Response[cask.Response.Data](
      downloadPageHtml,
      headers = Seq("Content-Type" -> "text/html; charset=utf-8")
    )

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

  /** The downloads that the page offers, as (system, label). */
  private val downloads = List(
    "windows" -> "Windows",
    "macos"   -> "macOS",
    "linux"   -> "Linux"
  )

  private val downloadPageHtml: String =
    "<!DOCTYPE html>" + html(
      lang := "en",
      head(
        meta(charset := "utf-8"),
        meta(name := "viewport", content := "width=device-width, initial-scale=1"),
        tag("title")("Battle for Flatland"),
        tag("style")(
          """body { font-family: sans-serif; max-width: 32em; margin: 3em auto; padding: 0 1em; text-align: center; }
            |a.download { display: block; margin: 1em 0; padding: 0.8em; border-radius: 0.4em; background: #2d6cdf; color: white; text-decoration: none; font-weight: bold; }
            |a.download:hover { background: #1f4fa8; }
            |""".stripMargin
        )
      ),
      body(
        h1("Battle for Flatland"),
        p("Download the game for your system, unpack it, and run it:"),
        for ((system, label) <- downloads)
          yield a(cls := "download", href := s"api/bff/client/$system")(s"Download for $label")
      )
    ).render
}
