package services.routing

import frontend.router.Router.router
import urldsl.language.{PathSegment, PathSegmentWithQueryParams}
import zio.{UIO, ZIO, ZLayer}

object FRouting {

  val serviceLive: Routing.Service = new Routing.Service {
    def moveTo(path: PathSegment[Unit, _]): UIO[Unit] = ZIO.effectTotal(
      router.moveTo("/" + path.createPath())
    )

    def moveTo[Q](pathAndQuery: PathSegmentWithQueryParams[Unit, _, Q, _])(q: Q): UIO[Unit] = ZIO.effectTotal {
      router.moveTo("/" + pathAndQuery.createUrlString((), q))
    }
  }

  final val live: ZLayer[Any, Nothing, Routing] = ZLayer.succeed(serviceLive)

}
