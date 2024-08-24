package services.routing

import urldsl.language.{PathSegment, PathSegmentWithQueryParams, QueryParameters}
import zio.{URIO, ZIO}

def moveTo(path: PathSegment[Unit, ?]): URIO[Routing, Unit] = ZIO.serviceWithZIO[Routing](_.moveTo(path))

def moveTo[Q](path: PathSegment[Unit, ?], query: QueryParameters[Q, ?])(q: Q): URIO[Routing, Unit] =
  ZIO.serviceWithZIO[Routing](_.moveTo(path, query)(q))

def moveTo[Q](pathWithQuery: PathSegmentWithQueryParams[Unit, ?, Q, ?])(q: Q): URIO[Routing, Unit] =
  ZIO.serviceWithZIO[Routing](_.moveTo(pathWithQuery)(q))
