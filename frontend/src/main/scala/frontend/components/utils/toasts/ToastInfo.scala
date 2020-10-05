package frontend.components.utils.toasts

import typings.reactToastify.typesMod.{ToastContent, ToastOptions => ReactToastOptions}
import services.toaster.ToastOptions

import scala.scalajs.js

final case class ToastInfo(content: String, toastLevel: ToastLevel, options: ToastOptions) {
  def toastContent: ToastContent = content.asInstanceOf[ToastContent]

  def reactToastOptions: ReactToastOptions = {
    val reactOptions = ReactToastOptions()

    options.autoClose.map(_.toMillis.toDouble).foreach(reactOptions.autoClose = _)
    options.onClose
      .map(f => ((_: js.Object) => utils.runtime.unsafeRunToFuture(f)): js.Function1[js.Object, Unit])
      .map(_.asInstanceOf[js.UndefOr[js.Function1[js.Object, Unit]]])
      .foreach(reactOptions.onClose = _)

    reactOptions
  }
}
