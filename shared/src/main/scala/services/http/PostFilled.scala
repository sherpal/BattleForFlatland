package services.http

import io.circe.{Decoder, Encoder}
import services.http.HttpClient.{Path, Query}
import zio.ZIO

final class PostFilled[R] private[http] () {

  def apply[Q](path: Path[Unit], query: Query[Q])(q: Q)(implicit decoder: Decoder[R]): ZIO[HttpClient, Throwable, R] =
    ZIO.serviceWithZIO[HttpClient](_.post[R](path, query)(q))

  def apply[B, Q](
      path: Path[Unit],
      query: Query[Q],
      body: B
  )(q: Q)(using Decoder[R], Encoder[B]): ZIO[HttpClient, Throwable, R] =
    ZIO.serviceWithZIO[HttpClient](_.post[R](path, query, body)(q))

  def apply[B](
      path: Path[Unit],
      body: B
  )(using Decoder[R], Encoder[B]): ZIO[HttpClient, Throwable, R] =
    ZIO.serviceWithZIO[HttpClient](_.post[R](path, body))

}
