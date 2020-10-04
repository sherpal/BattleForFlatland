package frontend.components.utils.toasts

sealed trait ToastLevel

object ToastLevel {
  case object Info extends ToastLevel
  case object Warning extends ToastLevel
  case object Error extends ToastLevel
  case object Success extends ToastLevel
  case object Dark extends ToastLevel
}
