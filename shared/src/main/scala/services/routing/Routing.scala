package services.routing

import urldsl.language.{PathSegment, PathSegmentWithQueryParams, QueryParameters}
import zio.UIO

object Routing {

  trait Service {
    def moveTo(path: PathSegment[Unit, _]): UIO[Unit]

    def moveTo[Q](path: PathSegment[Unit, _], query: QueryParameters[Q, _])(q: Q): UIO[Unit] = moveTo(path ? query)(q)

    def moveTo[Q](pathAndQuery: PathSegmentWithQueryParams[Unit, _, Q, _])(q: Q): UIO[Unit]
  }
}
