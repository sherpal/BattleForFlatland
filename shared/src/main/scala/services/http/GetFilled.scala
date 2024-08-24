package services.http

import io.circe.Decoder
import services.http.HttpClient.{Path, Query}
import zio.ZIO

final class GetFilled[R] private[http] () {

  def apply[T, Q](path: Path[T], query: Query[Q])(t: T, q: Q)(implicit
      decoder: Decoder[R]
  ): ZIO[HttpClient, Throwable, R] = ZIO.serviceWithZIO[HttpClient](_.get[R](path, query)(t, q))

  def apply(path: Path[Unit])(implicit decoder: Decoder[R]): ZIO[HttpClient, Throwable, R] =
    ZIO.serviceWithZIO[HttpClient](_.get[R](path))

  def apply[Q](path: Path[Unit], query: Query[Q])(q: Q)(implicit decoder: Decoder[R]): ZIO[HttpClient, Throwable, R] =
    ZIO.serviceWithZIO[HttpClient](_.get[R](path, query)(q))

}
