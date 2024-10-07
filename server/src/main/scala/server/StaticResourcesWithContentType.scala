package server

import cask.model.Request
import cask.model.Response.Raw
import cask.router.Result

final class StaticResourcesWithContentType(
    path: String,
    default: => cask.Response[cask.Response.Data]
) extends cask.staticResources(path) {
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
          case other =>
            throw new UnsupportedOperationException(
              s"Don't know extension $other"
            )
        }
        super
          .wrapFunction(ctx, delegate)
          .map(result =>
            result.copy(headers =
              result.headers.filterNot(
                _._1.toLowerCase() == "content-type"
              ) :+ ("Content-Type" -> contentType)
            )
          )
    }

}
