package services.toaster

import zio.ZIO

import scala.concurrent.duration.FiniteDuration

/**
  * All options that can be set for toasters.
  *
  * All arguments are wrapped in `Option`. If set to None (which is the default for all of them), then it is the
  * default behaviour of the library which is used.
  *
  * @param autoClose Right the duration the toast has to stay alive, or Left false for no autoClose.
  * @param onClose zio effect to run when the toast disappears
  * @param hideProgressBar whether to hide the progress bar of the tooltip
  */
final case class ToastOptions(
    autoClose: Option[Either[false, FiniteDuration]]                        = None,
    onClose: Option[ZIO[utils.ziohelpers.FrontendGlobalEnv, Nothing, Unit]] = None,
    hideProgressBar: Option[Boolean]                                        = None
)

object ToastOptions {

  def empty: ToastOptions = ToastOptions()

}
