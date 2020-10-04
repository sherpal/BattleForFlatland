package frontend.components.utils

import com.raquo.airstream.eventbus.{EventBus, WriteBus}
import typings.reactToastify.mod.toast
import typings.reactToastify.typesMod.ToastContent

import scala.scalajs.js

package object toasts {

  private[toasts] val toastBus: EventBus[ToastInfo] = new EventBus

  val toastWriter: WriteBus[ToastInfo] = toastBus.writer

  private[toasts] val toastEvents = toastBus.events

}
