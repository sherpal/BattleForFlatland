package services.toaster
import zio.{Has, UIO, ULayer, ZIO, ZLayer}
import frontend.components.utils.toasts.{toastWriter, ToastInfo, ToastLevel}

object FToaster {

  object Service {

    def live: Toaster.Service = new Toaster.Service {

      def toast(content: String, level: ToastLevel, options: => ToastOptions): UIO[Unit] =
        ZIO.effect {
          toastWriter.onNext(ToastInfo(content, level, options))
        }.ignore

      def info(content: String, options: => ToastOptions): UIO[Unit]    = toast(content, ToastLevel.Info, options)
      def success(content: String, options: => ToastOptions): UIO[Unit] = toast(content, ToastLevel.Success, options)
      def warn(content: String, options: => ToastOptions): UIO[Unit]    = toast(content, ToastLevel.Warning, options)
      def error(content: String, options: => ToastOptions): UIO[Unit]   = toast(content, ToastLevel.Error, options)
    }

  }

  def live: ULayer[Toaster] = ZLayer.succeed(Service.live)

}
