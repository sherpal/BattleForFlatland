package services.toaster

import zio.{UIO, URIO, ZIO}

final class Toast {

  def info(content: String, options: => ToastOptions = ToastOptions.empty): URIO[Toaster, Unit] =
    ZIO.accessM(_.get[Toaster.Service].info(content, options))
  def success(content: String, options: => ToastOptions = ToastOptions.empty): URIO[Toaster, Unit] =
    ZIO.accessM(_.get[Toaster.Service].success(content, options))
  def warn(content: String, options: => ToastOptions = ToastOptions.empty): URIO[Toaster, Unit] =
    ZIO.accessM(_.get[Toaster.Service].warn(content, options))
  def error(content: String, options: => ToastOptions = ToastOptions.empty): URIO[Toaster, Unit] =
    ZIO.accessM(_.get[Toaster.Service].error(content, options))

}
