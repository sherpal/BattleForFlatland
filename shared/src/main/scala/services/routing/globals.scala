package services.routing

import urldsl.language.{PathSegment, PathSegmentWithQueryParams, QueryParameters}
import urldsl.language.dummyErrorImpl.*
import zio.{URIO, ZIO}

val baseStr     = baseStrSpecific
inline def base = root / baseStr.filterNot(_ == '/')

def moveTo(path: PathSegment[Unit, ?]): URIO[Routing, Unit] =
  ZIO.serviceWithZIO[Routing](_.moveTo(path))

def moveTo[Q](path: PathSegment[Unit, ?], query: QueryParameters[Q, ?])(q: Q): URIO[Routing, Unit] =
  ZIO.serviceWithZIO[Routing](_.moveTo(path, query)(q))

def moveTo[Q](pathWithQuery: PathSegmentWithQueryParams[Unit, ?, Q, ?])(q: Q): URIO[Routing, Unit] =
  ZIO.serviceWithZIO[Routing](_.moveTo(pathWithQuery)(q))

def currentUrlMatches(matcher: urldsl.language.UrlPart[?, ?]): URIO[Routing, Boolean] =
  ZIO.serviceWithZIO[Routing](_.currentUrlMatches(matcher))
