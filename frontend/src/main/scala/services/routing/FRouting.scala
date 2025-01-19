package services.routing

import components.router.Router
import urldsl.language.PathSegment
import zio.*
import urldsl.language.PathSegmentWithQueryParams
import urldsl.language.UrlPart

class FRouting(router: Router) extends Routing {

  override def currentUrlMatches(matcher: UrlPart[?, ?]): UIO[Boolean] =
    ZIO.succeed(router.currentUrlMatches(matcher))

  override def moveTo[Q](pathAndQuery: PathSegmentWithQueryParams[Unit, ?, Q, ?])(q: Q): UIO[Unit] =
    ZIO.succeed(router.moveTo("/" ++ pathAndQuery.createUrlString((), q)))

  override def moveTo(path: PathSegment[Unit, ?]): UIO[Unit] =
    ZIO.succeed(router.moveTo("/" ++ path.createPath()))

}

object FRouting {
  def live = ZLayer.fromZIO(for {
    _        <- Console.printLine("Initializing Routing service...").orDie
    router   <- ZIO.succeed(Router.router)
    frouting <- ZIO.succeed(FRouting(router))
  } yield (frouting: Routing))
}
