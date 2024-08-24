package services.http

import io.circe.{Decoder, Encoder}
import urldsl.errors.DummyError
import urldsl.language.{PathSegment, QueryParameters}
import zio.{Task, UIO}
import services.http.HttpClient.{Path, Query}

trait HttpClient {

  /** Returns the csrf token cookie if it is set, None otherwise. */
  def maybeCsrfToken: UIO[Option[String]]

  trait GETResponseFilled[R] {

    /** Makes a GET http call to the given [[Path]] with the given [[Query]] parameters. Interpret the response as an
      * element of type `R`.
      */
    def apply[T, Q](path: Path[T], query: Query[Q])(t: T, q: Q)(using decoder: Decoder[R]): Task[R]

    /** Makes a GET http call to the given [[Path]]. Interpret the response as an element of type `R`.
      */
    def apply(path: Path[Unit])(using decoder: Decoder[R]): Task[R]

    /** Makes a GET http call to the given [[Path]] with the given [[Query]] parameters. Interpret the reponse as an
      * element of type `R`.
      */
    def apply[Q](path: Path[Unit], query: Query[Q])(q: Q)(using decoder: Decoder[R]): Task[R]

  }

  def get[R]: GETResponseFilled[R]

  /** Makes a GET http call to the given [[Path]] and return the status code.
    */
  def getStatus(path: Path[Unit]): Task[Int]

  trait POSTResponseFilled[R] {

    /** Makes a POST http call to the given [[Path]] with the given [[Query]] parameters, without body. Interpret the
      * response as an element of type `R`.
      */
    def apply[Q](path: Path[Unit], query: Query[Q])(q: Q)(using decoder: Decoder[R]): Task[R]

    /** Makes a POST http call to the given [[Path]] with the given [[Query]] parameters and with the given body.
      * Interpret the response as an element of type `R`.
      */
    def apply[B, Q](path: Path[Unit], query: Query[Q], body: B)(
        q: Q
    )(using decoder: Decoder[R], encoder: Encoder[B]): Task[R]

    def apply[B](path: Path[Unit], body: B)(using decoder: Decoder[R], encoder: Encoder[B]): Task[R]
  }

  def post[R]: POSTResponseFilled[R]

  /** Makes a POST http call to the given [[Path]] with the given [[Query]] parameters, without body. Ignores the
    * response body, and returns the status code instead.
    */
  def postIgnore[Q](path: Path[Unit], query: Query[Q])(q: Q): Task[Int]

  /** Makes a POST http call to the given [[Path]] with the given body of type `B`. Ignores the response body, and
    * returns the status code instead.
    */
  def postIgnore[B](path: Path[Unit], body: B)(using encoder: Encoder[B]): Task[Int]

  /** Makes a POST http call to the given [[Path]] with the given [[Query]] parameters and with the given body. Ignores
    * the response body, and returns the status code instead.
    */
  def postIgnore[B, Q](path: Path[Unit], query: Query[Q], body: B)(q: Q)(using encoder: Encoder[B]): Task[Int]
}

object HttpClient {
  final val apiPrefix: String     = "api"
  final val csrfTokenName: String = "Csrf-Token"

  type Path[T]  = PathSegment[T, DummyError]
  type Query[Q] = QueryParameters[Q, DummyError]

}
