package services.http

import io.circe.{Decoder, Encoder}
import urldsl.errors.DummyError
import urldsl.language.{PathSegment, QueryParameters}
import zio._

/**
  * The [[HttpClient]] service allows to make http calls to the server.
  */
object HttpClient {

  final val apiPrefix: String     = "api"
  final val csrfTokenName: String = "Csrf-Token"

  type Path[T]  = PathSegment[T, DummyError]
  type Query[Q] = QueryParameters[Q, DummyError]

  trait Service {

    /** Returns the csrf token cookie if it is set, None otherwise. */
    def maybeCsrfToken: UIO[Option[String]]

    /**
      * Makes a GET http call to the given [[Path]] with the given [[Query]] parameters.
      * Interpret the response as an element of type `R`.
      */
    def get[T, Q, R](path: Path[T], query: Query[Q])(t: T, q: Q)(implicit decoder: Decoder[R]): Task[R]

    /**
      * Makes a GET http call to the given [[Path]].
      * Interpret the response as an element of type `R`.
      */
    def get[R](path: Path[Unit])(implicit decoder: Decoder[R]): Task[R]

    /**
      * Makes a GET http call to the given [[Path]] with the given [[Query]] parameters.
      * Interpret the reponse as an element of type `R`.
      */
    def get[Q, R](path: Path[Unit], query: Query[Q])(q: Q)(implicit decoder: Decoder[R]): Task[R]

    /**
      * Makes a GET http call to the given [[Path]] and return the status code.
      */
    def getStatus(path: Path[Unit]): Task[Int]

    /**
      * Makes a POST http call to the given [[Path]] with the given [[Query]] parameters, without body.
      * Interpret the response as an element of type `R`.
      */
    def post[Q, R](path: Path[Unit], query: Query[Q])(q: Q)(implicit decoder: Decoder[R]): Task[R]

    /**
      * Makes a POST http call to the given [[Path]] with the given [[Query]] parameters, without body.
      * Ignores the response body, and returns the status code instead.
      */
    def postIgnore[Q](path: Path[Unit], query: Query[Q])(q: Q): Task[Int]

    /**
      * Makes a POST http call to the given [[Path]] with the given body of type `B`.
      * Ignores the response body, and returns the status code instead.
      */
    def postIgnore[B](path: Path[Unit], body: B)(implicit encoder: Encoder[B]): Task[Int]

    /**
      * Makes a POST http call to the given [[Path]] with the given [[Query]] parameters and with the given body.
      * Interpret the response as an element of type `R`.
      */
    def post[B, Q, R](path: Path[Unit], query: Query[Q], body: B)(
        q: Q
    )(implicit decoder: Decoder[R], encoder: Encoder[B]): Task[R]

    /**
      * Makes a POST http call to the given [[Path]] with the given [[Query]] parameters and with the given body.
      * Ignores the response body, and returns the status code instead.
      */
    def postIgnore[B, Q](path: Path[Unit], query: Query[Q], body: B)(q: Q)(implicit encoder: Encoder[B]): Task[Int]
  }

}
