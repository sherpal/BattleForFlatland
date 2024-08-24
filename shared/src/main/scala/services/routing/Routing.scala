package services.routing

import urldsl.language.{PathSegment, PathSegmentWithQueryParams, QueryParameters}
import zio.UIO

trait Routing {
  def moveTo(path: PathSegment[Unit, ?]): UIO[Unit]

  def moveTo[Q](path: PathSegment[Unit, ?], query: QueryParameters[Q, ?])(q: Q): UIO[Unit] = moveTo(path ? query)(q)

  def moveTo[Q](pathAndQuery: PathSegmentWithQueryParams[Unit, ?, Q, ?])(q: Q): UIO[Unit]
}
