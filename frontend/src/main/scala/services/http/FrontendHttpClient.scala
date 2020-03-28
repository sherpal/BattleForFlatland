package services.http

import errors.ErrorADT
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Error}
import org.scalajs.dom
import services.http.HttpClient.{csrfTokenName, Path, Query, Service}
import sttp.client._
import sttp.model.{MediaType, MultiQueryParams, Uri}
import zio._

import scala.concurrent.Future

object FrontendHttpClient {

  val live: ZLayer[Any, Nothing, HttpClient] = ZLayer.succeed(new Service {
    implicit def bodySerializer[A](implicit aEncoder: Encoder[A]): A => BasicRequestBody =
      (a: A) =>
        StringBody(
          a.asJson.noSpaces,
          "utf-8",
          Some(MediaType.ApplicationJson)
        )

    implicit val backend: SttpBackend[Future, Nothing, NothingT] = FetchBackend()

    def maybeCsrfToken: UIO[Option[String]] = ZIO.succeed(
      dom.document.cookie
        .split(";")
        .map(_.trim)
        .find(_.startsWith(s"$csrfTokenName="))
        .map(_.drop(csrfTokenName.length + 1))
    )

    private def pathWithParams[T, Q](path: Path[T], query: Query[Q])(t: T, q: Q): UIO[Uri] =
      host
        .map(_.path(HttpClient.apiPrefix +: path.createSegments(t).map(_.content)))
        .map(_.params(MultiQueryParams.fromMultiMap(query.createParamsMap(q))))

    private def simplePath[T](p: Path[T])(t: T): UIO[Uri] =
      host.map(_.path(HttpClient.apiPrefix +: p.createSegments(t).map(_.content)))

    private def host: UIO[Uri] = ZIO.succeed(
      Uri.parse(dom.document.location.origin.toString).toOption.get
    )

    private def boilerplate: UIO[RequestT[Empty, Either[String, String], Nothing]] =
      maybeCsrfToken.map(token => basicRequest.header(csrfTokenName, token.getOrElse("none")))

    def responseAs[E, A](
        implicit aDecoder: Decoder[A],
        eDecoder: Decoder[E]
    ): ResponseAs[Either[Either[Error, E], Either[Error, A]], Nothing] = asEither(
      asStringAlways.map(decode[E]),
      asStringAlways.map(
        decode[A]
      )
    )

    def errorResponse[E](implicit eDecoder: Decoder[E]): ResponseAs[Option[Either[Error, E]], Nothing] =
      asEither(
        asStringAlways.map(decode[E]),
        ignore
      ).map(_.swap.toOption)

    def readResult[E <: Throwable, A](response: Response[Either[Either[Error, E], Either[Error, A]]]): IO[E, A] =
      ZIO
        .effect(response.body match {
          case Right(Right(t)) => Right[E, A](t)
          case Right(Left(e))  => throw e // todo: change this nonsense
          case Left(Left(e))   => throw e
          case Left(Right(e))  => Left[E, A](e)
        })
        .orDie
        .flatMap {
          case Left(e)  => ZIO.fail(e)
          case Right(a) => ZIO.succeed(a)
        }

    private def preparedQuery[A](implicit decoder: Decoder[A]) =
      for {
        start <- boilerplate
        uri <- ZIO.environment[Uri]
        response <- Task
          .fromFuture(
            implicit ec => start.response(responseAs[ErrorADT, A]).get(uri).send()
          )
          .orDie
        resultBody <- readResult(response)
      } yield resultBody

    private def preparedPostQuery[B, R](
        body: Option[B]
    )(implicit decoder: Decoder[R], encoder: Encoder[B]) =
      for {
        start <- boilerplate
        uri <- ZIO.environment[Uri]
        response <- Task
          .fromFuture(
            implicit ec => {
              val withResponseAndUri = start.response(responseAs[ErrorADT, R]).post(uri)
              body match {
                case Some(b) => withResponseAndUri.body(b).send()
                case None    => withResponseAndUri.send()
              }
            }
          )
          .orDie
        resultBody <- readResult(response)
      } yield resultBody

    private def preparedPostQueryIgnore[B](body: Option[B])(implicit encoder: Encoder[B]) =
      for {
        start <- boilerplate
        uri <- ZIO.environment[Uri]
        response <- Task
          .fromFuture(
            implicit ec => {
              val withResponseAndUri = start.response(errorResponse[ErrorADT]).post(uri)
              body match {
                case Some(b) => withResponseAndUri.body(b).send()
                case None    => withResponseAndUri.send()
              }
            }
          )
          .orDie
        _ <- response.body match {
          case None               => ZIO.succeed(())
          case Some(Right(error)) => ZIO.fail(error)
          case Some(Left(e))      => ZIO.fail(e)
        }
      } yield response.code.code

    def get[T, Q, R](path: Path[T], query: Query[Q])(t: T, q: Q)(implicit decoder: Decoder[R]): Task[R] =
      pathWithParams(path, query)(t, q).flatMap(preparedQuery[R].provide)

    def get[R](path: Path[Unit])(implicit decoder: Decoder[R]): Task[R] =
      simplePath(path)(()).flatMap(preparedQuery[R].provide)

    def get[Q, R](path: Path[Unit], query: Query[Q])(q: Q)(implicit decoder: Decoder[R]): Task[R] =
      pathWithParams(path, query)((), q).flatMap(preparedQuery[R].provide)

    def getStatus(path: Path[Unit]): Task[Int] =
      for {
        start <- boilerplate
        uri <- simplePath(path)(())
        response <- ZIO.fromFuture { implicit ec =>
          start.response(ignore).get(uri).send()
        }
        statusCode = response.code.code
      } yield statusCode

    def post[Q, R](path: Path[Unit], query: Query[Q])(q: Q)(implicit decoder: Decoder[R]): Task[R] =
      pathWithParams(path, query)((), q).flatMap(preparedPostQuery[Int, R](None).provide)

    def post[B, Q, R](path: Path[Unit], query: Query[Q], body: B)(
        q: Q
    )(implicit decoder: Decoder[R], encoder: Encoder[B]): Task[R] =
      pathWithParams(path, query)((), q).flatMap(preparedPostQuery(Some(body)).provide)

    def postIgnore[Q](path: Path[Unit], query: Query[Q])(q: Q): Task[Int] =
      pathWithParams(path, query)((), q).flatMap(preparedPostQueryIgnore[Int](None).provide)

    def postIgnore[B](path: Path[Unit], body: B)(implicit encoder: Encoder[B]): Task[Int] =
      simplePath(path)(()).flatMap(preparedPostQueryIgnore(Some(body)).provide)

    def postIgnore[B, Q](path: Path[Unit], query: Query[Q], body: B)(q: Q)(implicit encoder: Encoder[B]): Task[Int] =
      pathWithParams(path, query)((), q).flatMap(preparedPostQueryIgnore(Some(body)).provide)
  })

}
