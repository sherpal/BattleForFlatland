package server

import java.nio.charset.StandardCharsets

import services.i18n.Language

/** The website: the [[LandingPage]] in each language (`/en`, `/fr`...), its pictures (the
  * [[LandingAssets]]), and the root, which goes to the page in the visitor's language.
  *
  * A route per language, since cask does not let a `/:language` route live next to the others (like
  * `/api`): a new [[Language]] needs its own.
  *
  * Everything has an ETag (the hash of its content), so that the browsers that have it already get
  * a 304 (Not Modified). The browsers keep the pages a few minutes (a new version is seen soon
  * after a deploy), and the pictures for good, since their address changes with them.
  */
class LandingRoutes extends cask.Routes {
  import LandingRoutes.*

  @cask.get("/")
  def root(request: cask.Request): cask.Response.Raw =
    cask.Response[cask.Response.Data](
      "",
      statusCode = 302,
      headers = Seq(
        "Location" -> s"/${preferredLanguage(request.headers.get("accept-language")).code}",
        "Vary"     -> "Accept-Language"
      )
    )

  @cask.get("/en")
  def english(request: cask.Request): cask.Response.Raw = page(Language.English, request)

  @cask.get("/fr")
  def french(request: cask.Request): cask.Response.Raw = page(Language.French, request)

  @cask.get("/nl")
  def dutch(request: cask.Request): cask.Response.Raw = page(Language.Dutch, request)

  @cask.get("/de")
  def german(request: cask.Request): cask.Response.Raw = page(Language.German, request)

  @cask.get("/sq")
  def albanian(request: cask.Request): cask.Response.Raw = page(Language.Albanian, request)

  /** `/landing/<hash>/<name>`, as the page links them, or `/landing/<name>` (for links from
    * elsewhere). An old hash (from a page of before a deploy) gets the current version.
    */
  @cask.get("/landing", subpath = true)
  def asset(request: cask.Request): cask.Response.Raw = {
    val (hash, name) = request.remainingPathSegments match {
      case Seq(hash, name) => (Some(hash), name)
      case segments => (None, segments.mkString("/"))
    }
    LandingAssets(name) match {
      case None => cask.Response[cask.Response.Data]("Not found", statusCode = 404)
      case Some(asset) =>
        val cacheControl =
          if hash.contains(asset.hash) then "public, max-age=31536000, immutable"
          else "public, max-age=3600"
        cached(request, asset.hash, cacheControl, asset.contentType, asset.bytes)
    }
  }

  initialize()
}

object LandingRoutes {

  /** The pages, as bytes, with their hash. */
  private lazy val pages: Map[Language, (Array[Byte], String)] =
    Language.values.map { language =>
      val bytes = LandingPage(language).getBytes(StandardCharsets.UTF_8)
      language -> (bytes, LandingAssets.hash(bytes))
    }.toMap

  private def page(language: Language, request: cask.Request): cask.Response.Raw = {
    val (bytes, hash) = pages(language)
    cached(request, hash, "public, max-age=300", "text/html; charset=utf-8", bytes)
  }

  /** These bytes, with this ETag (a hash of them): a 304 (Not Modified) if the browser has them
    * already, or the part of them that it asks for (a `Range`: browsers ask parts of videos).
    */
  private def cached(
      request: cask.Request,
      hash: String,
      cacheControl: String,
      contentType: String,
      bytes: Array[Byte]
  ): cask.Response.Raw = {
    val etag    = s"\"$hash\""
    val headers = Seq("ETag" -> etag, "Cache-Control" -> cacheControl)
    if request.headers.get("if-none-match").exists(_.exists(matches(_, etag))) then
      cask.Response[cask.Response.Data]("", statusCode = 304, headers = headers)
    else
      val content = headers ++ Seq("Content-Type" -> contentType, "Accept-Ranges" -> "bytes")
      request.headers.get("range").flatMap(_.headOption) match {
        case None =>
          cask.Response[cask.Response.Data](
            bytes,
            headers = content :+ ("Content-Length" -> bytes.length.toString)
          )
        case Some(header) =>
          range(header, bytes.length) match {
            case Some((from, to)) =>
              cask.Response[cask.Response.Data](
                bytes.slice(from, to + 1),
                statusCode = 206,
                headers = content ++ Seq(
                  "Content-Length" -> (to - from + 1).toString,
                  "Content-Range"  -> s"bytes $from-$to/${bytes.length}"
                )
              )
            case None =>
              cask.Response[cask.Response.Data](
                "",
                statusCode = 416,
                headers = headers :+ ("Content-Range" -> s"bytes */${bytes.length}")
              )
          }
      }
  }

  /** Whether an `If-None-Match` (e.g. `"abc", W/"def"`, or `*`) names this ETag. */
  def matches(ifNoneMatch: String, etag: String): Boolean =
    ifNoneMatch.split(',').map(_.trim.stripPrefix("W/")).exists(tag => tag == etag || tag == "*")

  /** The bytes (first and last, included) of a `Range` (`bytes=0-99`, `bytes=100-`, `bytes=-50`)
    * among `length`, if it is one range that we can give.
    */
  def range(header: String, length: Int): Option[(Int, Int)] = header.trim match {
    case s"bytes=$from-$to" if !from.contains(',') && !to.contains(',') =>
      (from.trim.toIntOption, to.trim.toIntOption) match {
        case (Some(from), Some(to)) if from <= to && from < length =>
          Some((from, math.min(to, length - 1)))
        case (Some(from), None) if to.trim.isEmpty && from < length => Some((from, length - 1))
        case (None, Some(last)) if from.trim.isEmpty && last > 0 && length > 0 =>
          Some((math.max(length - last, 0), length - 1))
        case _ => None
      }
    case _ => None
  }

  /** The visitor's favourite language among ours (from their `Accept-Language`, e.g.
    * `fr-BE,fr;q=0.9,en;q=0.8`), English by default.
    */
  def preferredLanguage(acceptLanguage: Option[collection.Seq[String]]): Language =
    acceptLanguage.toList
      .flatMap(_.flatMap(_.split(',')))
      .flatMap { entry =>
        entry.trim.split(';').toList match {
          case tagName :: parameters =>
            val quality = parameters.collectFirst { case s"q=$q" => q.trim.toDoubleOption }.flatten
            Some(tagName.trim.takeWhile(_ != '-').toLowerCase -> quality.getOrElse(1.0))
          case Nil => None
        }
      }
      .filter(_._2 > 0)
      .sortBy(-_._2)
      .flatMap((code, _) => Language.byCode(code))
      .headOption
      .getOrElse(Language.English)
}
