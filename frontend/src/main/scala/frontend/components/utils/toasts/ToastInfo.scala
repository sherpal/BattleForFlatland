package frontend.components.utils.toasts

import typings.reactToastify.typesMod.{ToastContent, ToastOptions => ReactToastOptions}
import services.toaster.ToastOptions
import zio.ZIO

import scala.scalajs.js
import scala.scalajs.js.|

final case class ToastInfo(content: String, toastLevel: ToastLevel, options: ToastOptions) {
  def toastContent: ToastContent = content.asInstanceOf[ToastContent]

  def reactToastOptions: ReactToastOptions = {
    val reactOptions = ReactToastOptions()

    def effectToJSFunction(
        zio: ZIO[utils.ziohelpers.FrontendGlobalEnv, Nothing, Unit]
    ): js.UndefOr[js.Function1[js.Object, Unit]] = {
      val f = ((_: js.Object) => utils.runtime.unsafeRunToFuture(zio)): js.Function1[js.Object, Unit]
      f.asInstanceOf[js.UndefOr[js.Function1[js.Object, Unit]]]
    }

    options.autoClose
      .map {
        case Left(_) =>
          typings.reactToastify.reactToastifyBooleans.`false`
            .asInstanceOf[Double | typings.reactToastify.reactToastifyBooleans.`false`]
        case Right(duration) =>
          duration.toMillis.toDouble.asInstanceOf[Double | typings.reactToastify.reactToastifyBooleans.`false`]
      }
      .foreach(reactOptions.autoClose = _)

    options.onClose.map(effectToJSFunction).foreach(reactOptions.onClose = _)
    options.onOpen.map(effectToJSFunction).foreach(reactOptions.onOpen   = _)

    options.hideProgressBar.foreach(reactOptions.hideProgressBar = _)
    options.pauseOnHover.foreach(reactOptions.pauseOnHover       = _)

    reactOptions
  }
}
