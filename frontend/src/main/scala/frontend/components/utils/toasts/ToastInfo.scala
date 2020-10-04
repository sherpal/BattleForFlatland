package frontend.components.utils.toasts

import typings.reactToastify.typesMod.ToastContent

final case class ToastInfo(content: String, toastLevel: ToastLevel) {
  def toastContent: ToastContent = content.asInstanceOf[ToastContent]
}
