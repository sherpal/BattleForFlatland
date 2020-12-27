package services.http

import errors.ErrorADT
import io.circe.{Decoder, Encoder}
import org.scalajs.dom
import org.scalajs.dom.XMLHttpRequest
import services.http.HttpClient.{apiPrefix, csrfTokenName, Path, Query, Service}
import urldsl.language.{PathSegment, QueryParameters}
import zio.{Task, UIO, ZIO, ZLayer}
import io.circe.parser.decode

object FHttpClient {

  val serviceLive: HttpClient.Service = new Service {
    def maybeCsrfToken: UIO[Option[String]] = ZIO.succeed(
      dom.document.cookie
        .split(";")
        .map(_.trim)
        .find(_.startsWith(s"$csrfTokenName="))
        .map(_.drop(csrfTokenName.length + 1))
    )

    private final class RequestFailed(code: Int) extends Exception(s"Error code: $code")

    def decoderToBodyReader[R](decoder: Decoder[R]): String => Either[io.circe.Error, R] = decode(_)(decoder)

    class RequestForResponse[R] {
      def apply[Q](
          method: String,
          path: PathSegment[Unit, _],
          maybeQuery: Option[(QueryParameters[Q, _], Q)],
          body: Option[String],
          bodyReader: String => Either[io.circe.Error, R]
      ): ZIO[Any, Throwable, (R, Int)] =
        apply(method, path, maybeQuery, body, (), bodyReader)

      def apply[T, Q](
          method: String,
          path: PathSegment[T, _],
          maybeQuery: Option[(QueryParameters[Q, _], Q)],
          body: Option[String],
          pathArg: T,
          bodyReader: String => Either[io.circe.Error, R]
      ): ZIO[Any, Throwable, (R, Int)] =
        for {
          request <- UIO(new XMLHttpRequest)

          responseTextFiber <- ZIO
            .effectAsync[Any, Throwable, (R, Int)] { callback =>
              request.onreadystatechange = (_: dom.Event) => {
                if (request.readyState == 4 && (request.status / 200 <= 1)) {
                  callback(
                    ZIO.fromEither(
                      bodyReader(request.response.asInstanceOf[String]).map(_ -> request.status)
                    )
                  )
                } else if (request.readyState == 4) {
                  callback(
                    ZIO
                      .fromEither(decode[ErrorADT](request.response.asInstanceOf[String]))
                      .either
                      .map(_.fold[Throwable](identity, identity))
                      .flatMap(ZIO.fail[Throwable](_))
                  )
                }
              }
            }
            .fork
          queryString = maybeQuery.fold("") {
            case (query, q) => "?" ++ query.createParamsString(q)
          }
          maybeCsrf <- maybeCsrfToken
          _ <- ZIO.effectTotal {
            request.open(
              method,
              dom.document.location.origin.toString + s"/$apiPrefix/" + path.createPath(pathArg) + queryString,
              async = true
            )

            maybeCsrf.filter(_ => method != "GET").foreach { token =>
              request.setRequestHeader(csrfTokenName, token)
            }
            body match {
              case Some(b) =>
                request.setRequestHeader("Content-Type", "application/json")
                request.send(b)
              case None => request.send()
            }
          }
          response <- responseTextFiber.join
        } yield response

    }

    private def send[R] = new RequestForResponse[R]

    def getStatus(path: Path[Unit]): Task[Int] =
      send[Unit]("GET", path, None, None, _ => Right(())).map(_._2)

    def get[R]: GETResponseFilled[R] = new GETResponseFilled[R] {
      def apply[T, Q](path: Path[T], query: Query[Q])(t: T, q: Q)(implicit decoder: Decoder[R]): Task[R] =
        send[R]("GET", path, Some((query, q)), None, t, decoderToBodyReader(decoder)).map(_._1)

      def apply(path: Path[Unit])(implicit decoder: Decoder[R]): Task[R] =
        send[R]("GET", path, None, None, decoderToBodyReader(decoder)).map(_._1)

      def apply[Q](path: Path[Unit], query: Query[Q])(q: Q)(implicit decoder: Decoder[R]): Task[R] =
        send[R]("GET", path, Some((query, q)), None, decoderToBodyReader(decoder)).map(_._1)
    }

    def post[R]: POSTResponseFilled[R] = new POSTResponseFilled[R] {
      def apply[Q](path: Path[Unit], query: Query[Q])(q: Q)(implicit decoder: Decoder[R]): Task[R] =
        send[R]("POST", path, Some((query, q)), None, decoderToBodyReader(decoder)).map(_._1)

      def apply[B, Q](path: Path[Unit], query: Query[Q], body: B)(
          q: Q
      )(implicit decoder: Decoder[R], encoder: Encoder[B]): Task[R] =
        send[R]("POST", path, Some((query, q)), Some(encoder(body).noSpaces), decoderToBodyReader(decoder)).map(_._1)

      def apply[B](path: Path[Unit], body: B)(implicit decoder: Decoder[R], encoder: Encoder[B]): Task[R] =
        send[R]("POST", path, None, Some(encoder(body).noSpaces), decoderToBodyReader(decoder)).map(_._1)
    }

    def postIgnore[Q](path: Path[Unit], query: Query[Q])(q: Q): Task[Int] =
      send[Unit]("POST", path, Some((query, q)), None, _ => Right(())).map(_._2)

    def postIgnore[B](path: Path[Unit], body: B)(implicit encoder: Encoder[B]): Task[Int] =
      send[Unit]("POST", path, None, Some(encoder(body).noSpaces), _ => Right(())).map(_._2)

    def postIgnore[B, Q](path: Path[Unit], query: Query[Q], body: B)(q: Q)(implicit encoder: Encoder[B]): Task[Int] =
      send[Unit]("POST", path, Some((query, q)), Some(encoder(body).noSpaces), _ => Right(())).map(_._2)

  }

  val live: ZLayer[Any, Nothing, HttpClient] = ZLayer.succeed(serviceLive)

}
