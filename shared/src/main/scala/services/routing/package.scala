package services

import urldsl.language.{PathSegment, PathSegmentWithQueryParams, QueryParameters}
import zio.{Has, URIO, ZIO}

package object routing {

  type Routing = Has[Routing.Service]

  def moveTo(path: PathSegment[Unit, ?]): URIO[Routing, Unit] = ZIO.accessM(_.get[Routing.Service].moveTo(path))

  def moveTo[Q](path: PathSegment[Unit, ?], query: QueryParameters[Q, ?])(q: Q): URIO[Routing, Unit] =
    ZIO.accessM(_.get[Routing.Service].moveTo(path, query)(q))

  def moveTo[Q](pathWithQuery: PathSegmentWithQueryParams[Unit, ?, Q, ?])(q: Q): URIO[Routing, Unit] =
    ZIO.accessM(_.get[Routing.Service].moveTo(pathWithQuery)(q))

}
