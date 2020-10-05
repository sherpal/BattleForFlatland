package services.toaster

import zio.UIO

object Toaster {

  trait Service {

    def info(content: String, options: => ToastOptions): UIO[Unit]
    def success(content: String, options: => ToastOptions): UIO[Unit]
    def warn(content: String, options: => ToastOptions): UIO[Unit]
    def error(content: String, options: => ToastOptions): UIO[Unit]

  }

}
