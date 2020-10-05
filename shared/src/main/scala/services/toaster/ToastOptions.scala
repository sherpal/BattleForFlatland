package services.toaster

import models.syntax.Pointed
import zio.ZIO

import scala.concurrent.duration.FiniteDuration

final case class ToastOptions(
    autoClose: Option[FiniteDuration]                                       = None,
    onClose: Option[ZIO[utils.ziohelpers.FrontendGlobalEnv, Nothing, Unit]] = None
)

object ToastOptions {

  def empty: ToastOptions = Pointed[ToastOptions].unit

}
