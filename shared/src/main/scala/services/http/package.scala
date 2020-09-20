package services

import errors.ErrorADT
import io.circe.{Decoder, Encoder}
import services.http.HttpClient.{Path, Query}
import zio.{Has, Task, UIO, ZIO}

package object http {

  type HttpClient = Has[HttpClient.Service]

  def get[T, Q, R](path: Path[T], query: Query[Q])(t: T, q: Q)(
      implicit decoder: Decoder[R]
  ): ZIO[HttpClient, Throwable, R] = ZIO.accessM(_.get[HttpClient.Service].get[T, Q, R](path, query)(t, q))

  def get[R](path: Path[Unit])(implicit decoder: Decoder[R]): ZIO[HttpClient, Throwable, R] =
    ZIO.accessM(_.get[HttpClient.Service].get[R](path))

  def get[Q, R](path: Path[Unit], query: Query[Q])(q: Q)(implicit decoder: Decoder[R]): ZIO[HttpClient, Throwable, R] =
    ZIO.accessM(_.get[HttpClient.Service].get[Q, R](path, query)(q))

  /**
    * Similar to [[get]], but returns `default` when the status code is not 2xx.
    */
  def getOrElse[Q, R](path: Path[Unit], query: Query[Q])(q: Q, default: R)(
      implicit decoder: Decoder[R]
  ): ZIO[HttpClient, Throwable, R] =
    get[Q, R](path, query)(q).catchSome { case _: ErrorADT => UIO(default) }

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

  def getElsewhere[Q, R](path: Path[Unit], query: Query[Q], host: String, port: Int)(q: Q)(
      implicit decoder: Decoder[R]
  ): ZIO[HttpClient, Throwable, R] = ZIO.accessM(_.get[HttpClient.Service].getElsewhere(path, query, host, port)(q))

  def postElsewhere[B, Q, R](path: Path[Unit], query: Query[Q], body: B, host: String, port: Int)(
      q: Q
  )(implicit decoder: Decoder[R], encoder: Encoder[B]): ZIO[HttpClient, Throwable, R] = ZIO.accessM(
    _.get[HttpClient.Service].postElsewhere(path, query, body, host, port)(q)
  )

  def optionsElsewhere(path: Path[Unit], host: String, port: Int): ZIO[HttpClient, Throwable, Int] = ZIO.accessM(
    _.get[HttpClient.Service].optionsElsewhere(path, host, port)
  )

}
