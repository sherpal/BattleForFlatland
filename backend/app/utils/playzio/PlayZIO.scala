package utils.playzio

import errors.ErrorADT
import play.api.mvc._
import utils.playzio.ErrorsTransformer._
import zio.{Has, Runtime, Tagged, ZIO, ZLayer}

object PlayZIO {

  def zioRequest[R[_], A](implicit tagged: Tagged[HasRequest[R, A]]): ZIO[Has[HasRequest[R, A]], Nothing, R[A]] =
    ZIO.accessM(_.get.request)

  def simpleZIORequest[A](
      implicit tagged: Tagged[HasRequest[Request, A]]
  ): ZIO[Has[HasRequest[Request, A]], Nothing, Request[A]] =
    ZIO.accessM(_.get.request)

  final implicit class ZIOAction[R[_], B](actionBuilder: ActionBuilder[R, B]) {

    private def run[A](request: R[A], block: ZIO[Has[HasRequest[R, A]], ErrorADT, Result])(
        implicit tagged: Tagged[HasRequest[R, A]]
    ) =
      Runtime.default.unsafeRunToFuture(
        block
          .fold(_.result, identity)
          .provideLayer(ZLayer.succeed(HasRequest(request)))
      )

    def zio[A](
        bodyParser: BodyParser[A]
    )(block: ZIO[Has[HasRequest[R, A]], ErrorADT, Result])(implicit tagged: Tagged[HasRequest[R, A]]): Action[A] =
      actionBuilder.async(bodyParser) { run(_, block) }

    def zio(
        block: ZIO[Has[HasRequest[R, B]], ErrorADT, Result]
    )(implicit tagged: Tagged[HasRequest[R, B]]): Action[B] =
      actionBuilder.async { run(_, block) }

  }

  final implicit class ProvideButRequest[R, E](effect: ZIO[R, E, Result]) {
    def provideButRequest[Request[_], A]: ZIO.ProvideSomeLayer[Has[HasRequest[Request, A]], R, E, Result] =
      effect.provideSomeLayer[Has[HasRequest[Request, A]]]
  }

}
