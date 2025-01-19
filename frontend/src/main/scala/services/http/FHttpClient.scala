package services.http

import errors.ErrorADT
import io.circe.{Decoder, Encoder}
import org.scalajs.dom
import org.scalajs.dom.XMLHttpRequest
import services.http.HttpClient.{apiPrefix, csrfTokenName, Path, Query}
import urldsl.language.{PathSegment, QueryParameters}
import zio.{Task, UIO, ZIO, ZLayer}
import io.circe.parser.decode
import org.scalajs.dom.RequestInit
import org.scalajs.dom.HttpMethod

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

object FHttpClient {

  private def serviceLive(fetcher: Fetcher): HttpClient = new HttpClient {
    def maybeCsrfToken: UIO[Option[String]] = ZIO.succeed(
      dom.document.cookie
        .split(";")
        .map(_.trim)
        .find(_.startsWith(s"$csrfTokenName="))
        .map(_.drop(csrfTokenName.length + 1))
    )

    private final class RequestFailed(code: Int) extends Exception(s"Error code: $code")

    def decoderToBodyReader[R](decoder: Decoder[R]): String => Either[io.circe.Error, R] = {
      given Decoder[R] = decoder
      decode(_)
    }

    class RequestForResponse[R] {
      def apply[Q](
          method: HttpMethod,
          path: HttpClient.Path[Unit],
          maybeQuery: Option[(HttpClient.Query[Q], Q)],
          body: Option[String],
          bodyReader: String => Either[io.circe.Error, R]
      ): ZIO[Any, Throwable, (R, Int)] =
        apply(method, path, maybeQuery, body, (), bodyReader)

      def apply[T, Q](
          method: HttpMethod,
          path: HttpClient.Path[T],
          maybeQuery: Option[(HttpClient.Query[Q], Q)],
          maybeBody: Option[String],
          pathArg: T,
          bodyReader: String => Either[io.circe.Error, R]
      ): ZIO[Any, Throwable, (R, Int)] = for {
        _ <- ZIO.unit
        queryString = maybeQuery.fold("") { (query, q) =>
          "?" ++ query.createParamsString(q)
        }
        url = dom.document.location.origin.toString + s"/$apiPrefix/" + path.createPath(
          pathArg
        ) + queryString
        maybeCsrf <- maybeCsrfToken
        requestInit = {
          val ri = new RequestInit {}
          ri.method = method
          ri.body = maybeBody.orUndefined
          ri.headers = (
            Map() ++ maybeBody.fold(Map.empty[String, String])(_ =>
              Map("Content-Type" -> "application/json")
            ) ++ maybeCsrf.map(csrfTokenName -> _).filter(_ => method != HttpMethod.GET).toMap
          ).toJSDictionary

          ri
        }
        response <- ZIO.fromPromiseJS(fetcher.fetch(url, requestInit))
        text     <- ZIO.fromPromiseJS(response.text())
        status   <- ZIO.succeed(response.status)
        _ <- ZIO.unless(response.ok) {
          decode[ErrorADT](text) match {
            case Right(error) => ZIO.fail(error)
            case Left(_) =>
              ZIO.fail(
                RuntimeException(s"$status: Failed when calling $url. Response body was $text")
              )
          }
        }
        r <- ZIO.fromEither(bodyReader(text))
      } yield (r, status)

    }

    private def send[R] = new RequestForResponse[R]

    def getStatus(path: Path[Unit]): Task[Int] =
      send[Unit](HttpMethod.GET, path, None, None, _ => Right(())).map(_._2)

    def get[R]: GETResponseFilled[R] = new GETResponseFilled[R] {
      def apply[T, Q](path: Path[T], query: Query[Q])(t: T, q: Q)(implicit
          decoder: Decoder[R]
      ): Task[R] =
        send[R](HttpMethod.GET, path, Some((query, q)), None, t, decoderToBodyReader(decoder))
          .map(_._1)

      def apply(path: Path[Unit])(implicit decoder: Decoder[R]): Task[R] =
        send[R](HttpMethod.GET, path, None, None, decoderToBodyReader(decoder)).map(_._1)

      def apply[Q](path: Path[Unit], query: Query[Q])(q: Q)(implicit decoder: Decoder[R]): Task[R] =
        send[R](HttpMethod.GET, path, Some((query, q)), None, decoderToBodyReader(decoder))
          .map(_._1)
    }

    def post[R]: POSTResponseFilled[R] = new POSTResponseFilled[R] {
      def apply[Q](path: Path[Unit], query: Query[Q])(q: Q)(implicit decoder: Decoder[R]): Task[R] =
        send[R](HttpMethod.POST, path, Some((query, q)), None, decoderToBodyReader(decoder))
          .map(_._1)

      def apply[B, Q](path: Path[Unit], query: Query[Q], body: B)(
          q: Q
      )(implicit decoder: Decoder[R], encoder: Encoder[B]): Task[R] =
        send[R](
          HttpMethod.POST,
          path,
          Some((query, q)),
          Some(encoder(body).noSpaces),
          decoderToBodyReader(decoder)
        )
          .map(_._1)

      def apply[B](path: Path[Unit], body: B)(implicit
          decoder: Decoder[R],
          encoder: Encoder[B]
      ): Task[R] =
        send[R](
          HttpMethod.POST,
          path,
          None,
          Some(encoder(body).noSpaces),
          decoderToBodyReader(decoder)
        ).map(_._1)
    }

    def postIgnore[Q](path: Path[Unit], query: Query[Q])(q: Q): Task[Int] =
      send[Unit](HttpMethod.POST, path, Some((query, q)), None, _ => Right(())).map(_._2)

    def postIgnore[B](path: Path[Unit], body: B)(implicit encoder: Encoder[B]): Task[Int] =
      send[Unit](HttpMethod.POST, path, None, Some(encoder(body).noSpaces), _ => Right(()))
        .map(_._2)

    def postIgnore[B, Q](path: Path[Unit], query: Query[Q], body: B)(
        q: Q
    )(implicit encoder: Encoder[B]): Task[Int] =
      send[Unit](
        HttpMethod.POST,
        path,
        Some((query, q)),
        Some(encoder(body).noSpaces),
        _ => Right(())
      ).map(_._2)

  }

  val live = ZLayer.fromZIO(for {
    _       <- zio.Console.printLine("Initializing HttpClient Service...").orDie
    fetcher <- ZIO.succeed(Fetcher.domFetcher)
    service <- ZIO.succeed(serviceLive(fetcher))
  } yield service)

}
