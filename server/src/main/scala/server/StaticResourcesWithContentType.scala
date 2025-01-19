package server

import cask.model.Request
import cask.model.Response.Raw
import cask.router.Result
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributeView
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import cask.endpoints.StaticUtil
import java.nio.file.FileSystems
import java.net.URI
import scala.jdk.CollectionConverters.*

final class StaticResourcesWithContentType(
    path: String,
    default: => cask.Response[cask.Response.Data]
) extends cask.staticResources(path) {

  private lazy val fileSystem =
    FileSystems.newFileSystem(
      URI.create(getClass.getClassLoader.getResource("static/index.html").toString.split("!").head),
      Map().asJava
    )

  override def wrapFunction(ctx: Request, delegate: Delegate): Result[Raw] =
    ctx.remainingPathSegments.lastOption
      .filter(_.contains('.'))
      .map(_.reverse.takeWhile(_ != '.').reverse) match {
      case None => // need to server index.html here
        Result.Success(default)
      case Some(extension) =>
        val contentType = extension match {
          case "js"   => "application/javascript"
          case "css"  => "application/css"
          case "html" => "text/html; charset=utf-8"
          case "ttf"  => "font/ttf"
          case "png"  => "image/png"
          case "wav"  => "audio/wav"
          case other =>
            throw new UnsupportedOperationException(
              s"Don't know extension $other"
            )
        }

        delegate(Map()).map { t =>
          val fullPath = StaticUtil.makePathAndContentType(t, ctx)._1
          val maybeEtag = Try {
            val attributes = Files
              .getFileAttributeView(fileSystem.getPath(fullPath), classOf[BasicFileAttributeView])
              .readAttributes()

            attributes.size().toHexString ++ "-" ++ fullPath.hashCode().toHexString
          } match {
            case Success(value) =>
              Some(value)
            case Failure(exception) =>
              println(s"[DEBUG] Failed to build an etag for $fullPath")
              exception.printStackTrace()
              None
          }

          val maybeIfNoneMatch = ctx.headers.get("if-none-match").flatMap(_.headOption)

          (for {
            etag        <- maybeEtag
            ifNoneMatch <- maybeIfNoneMatch
            if ifNoneMatch == etag
          } yield cask.Response[cask.Response.Data]((), statusCode = 304))
            .getOrElse {
              cask.model.StaticResource(
                fullPath,
                getClass.getClassLoader,
                maybeEtag.map("ETag" -> _).toSeq :+ ("Content-Type" -> contentType)
              )
            }
        }

    }

}
