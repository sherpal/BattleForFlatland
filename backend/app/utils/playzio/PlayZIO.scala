package utils.playzio

import akka.stream.scaladsl.Flow
import errors.ErrorADT
import play.api.mvc.WebSocket.MessageFlowTransformer
import play.api.mvc._
import utils.playzio.ErrorsTransformer._
import zio.{Has, Runtime, ZIO, ZLayer}
import izumi.reflect.Tag

object PlayZIO {

  def zioRequest[R[_], A](implicit tagged: Tag[HasRequest[R, A]]): ZIO[Has[HasRequest[R, A]], Nothing, R[A]] =
    ZIO.accessM(_.get.request)

  def simpleZIORequest[A](
      implicit tagged: Tag[HasRequest[Request, A]]
  ): ZIO[Has[HasRequest[Request, A]], Nothing, Request[A]] =
    ZIO.accessM(_.get.request)

  def zioRequestHeader: ZIO[Has[RequestHeader], Nothing, RequestHeader] = ZIO.access(_.get[RequestHeader])

  final implicit class ZIOAction[R[_], B](actionBuilder: ActionBuilder[R, B]) {

    private def run[A](request: R[A], block: ZIO[Has[HasRequest[R, A]], ErrorADT, Result])(
        implicit tagged: Tag[HasRequest[R, A]]
    ) =
      Runtime.default.unsafeRunToFuture(
        block
          .fold(_.result, identity)
          .provideLayer(ZLayer.succeed(HasRequest(request)))
      )

    private def runForHeader(header: RequestHeader, block: ZIO[Has[RequestHeader], ErrorADT, Result]) =
      Runtime.default.unsafeRunToFuture(
        block.fold(_.result, identity).provideLayer(ZLayer.succeed(header))
      )

    def zio[A](
        bodyParser: BodyParser[A]
    )(block: ZIO[Has[HasRequest[R, A]], ErrorADT, Result])(implicit tagged: Tag[HasRequest[R, A]]): Action[A] =
      actionBuilder.async(bodyParser) { run(_, block) }

    def zio(
        block: ZIO[Has[HasRequest[R, B]], ErrorADT, Result]
    )(implicit tagged: Tag[HasRequest[R, B]]): Action[B] =
      actionBuilder.async { run(_, block) }

  }

  final implicit class ProvideButRequest[R, E](effect: ZIO[R, E, Result]) {
    def provideButRequest[Request[_], A]: ZIO.ProvideSomeLayer[Has[HasRequest[Request, A]], R, E, Result] =
      effect.provideSomeLayer[Has[HasRequest[Request, A]]]
  }

  final implicit class ZIOWebSocket(ws: WebSocket.type) {

    def zio[In, Out](
        f: ZIO[Has[RequestHeader], ErrorADT, Flow[In, Out, _]]
    )(implicit transformer: MessageFlowTransformer[In, Out]): WebSocket =
      ws.acceptOrResult[In, Out] { requestHeader =>
        Runtime.default.unsafeRunToFuture(f.mapError(_.result).either.provideLayer(ZLayer.succeed(requestHeader)))
      }

  }

  final implicit class ProvideButHeader[R, E, A](effect: ZIO[R, E, A]) {
    def provideButHeader: ZIO.ProvideSomeLayer[Has[RequestHeader], R, E, A] =
      effect.provideSomeLayer[Has[RequestHeader]]
  }

}
