package services

import io.circe.{Decoder, Encoder}
import services.http.HttpClient.{Path, Query}
import zio.{Has, ZIO}

package object http {

  type HttpClient = Has[HttpClient.Service]

  def get[T, Q, R](path: Path[T], query: Query[Q])(t: T, q: Q)(
      implicit decoder: Decoder[R]
  ): ZIO[HttpClient, Throwable, R] = ZIO.accessM(_.get[HttpClient.Service].get[T, Q, R](path, query)(t, q))

  def get[R](path: Path[Unit])(implicit decoder: Decoder[R]): ZIO[HttpClient, Throwable, R] =
    ZIO.accessM(_.get[HttpClient.Service].get[R](path))

  def get[Q, R](path: Path[Unit], query: Query[Q])(q: Q)(implicit decoder: Decoder[R]): ZIO[HttpClient, Throwable, R] =
    ZIO.accessM(_.get[HttpClient.Service].get[Q, R](path, query)(q))

  def getStatus(path: Path[Unit]): ZIO[HttpClient, Throwable, Int] =
    ZIO.accessM(_.get[HttpClient.Service].getStatus(path))

  def post[Q, R](path: Path[Unit], query: Query[Q])(q: Q)(implicit decoder: Decoder[R]): ZIO[HttpClient, Throwable, R] =
    ZIO.accessM(_.get[HttpClient.Service].post(path, query)(q))

  def postIgnore[Q](path: Path[Unit], query: Query[Q])(q: Q): ZIO[HttpClient, Throwable, Int] =
    ZIO.accessM(_.get[HttpClient.Service].postIgnore(path, query)(q))

  def postIgnore[B](path: Path[Unit], body: B)(implicit encoder: Encoder[B]): ZIO[HttpClient, Throwable, Int] =
    ZIO.accessM(_.get[HttpClient.Service].postIgnore(path, body))

  def post[B, Q, R](
      path: Path[Unit],
      query: Query[Q],
      body: B
  )(q: Q)(implicit decoder: Decoder[R], encoder: Encoder[B]): ZIO[HttpClient, Throwable, R] =
    ZIO.accessM(_.get[HttpClient.Service].post(path, query, body)(q))

  def post[B, R](
      path: Path[Unit],
      body: B
  )(implicit encoder: Encoder[B], decoder: Decoder[R]): ZIO[HttpClient, Throwable, R] =
    ZIO.accessM(_.get[HttpClient.Service].post(path, body))

  def postIgnore[B, Q](path: Path[Unit], query: Query[Q], body: B)(
      q: Q
  )(implicit encoder: Encoder[B]): ZIO[HttpClient, Throwable, Int] =
    ZIO.accessM(_.get[HttpClient.Service].postIgnore(path, query, body)(q))

}
