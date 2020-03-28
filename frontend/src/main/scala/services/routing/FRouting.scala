package services.routing

import urldsl.language.{PathSegment, PathSegmentWithQueryParams, QueryParameters}
import zio.{UIO, ZIO, ZLayer}
import frontend.router.Router.router

object FRouting {

  final val live: ZLayer[Any, Nothing, Routing] = ZLayer.succeed(new Routing.Service {
    def moveTo(path: PathSegment[Unit, _]): UIO[Unit] = ZIO.effectTotal(
      router.moveTo("/" + path.createPath())
    )

    def moveTo[Q](pathAndQuery: PathSegmentWithQueryParams[Unit, _, Q, _])(q: Q): UIO[Unit] = ZIO.effectTotal(
      router.moveTo("/" + pathAndQuery.createUrlString((), q))
    )
  })

}
