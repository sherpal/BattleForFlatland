package frontend.components.utils.toasts

import com.raquo.airstream.ownership.Subscription
import com.raquo.laminar.api.L.Owner
import org.scalajs.dom
import slinky.core.{Component, FunctionalComponent, StatelessComponent}
import slinky.core.annotations.react
import slinky.core.facade.{React, ReactElement}
import slinky.web.html._
import typings.reactToastify
import typings.reactToastify.mod.toast

import scala.scalajs.js

@react final class Toaster extends Component {

  type Props = Unit
  type State = Option[Subscription]

  override def initialState: Option[Subscription] = Option.empty

  implicit private val owner = new Owner {}

  override def componentDidMount(): Unit =
    setState(
      Some(
        toastEvents.foreach { content =>
          content.toastLevel match {
            case ToastLevel.Info    => toast.info(content.toastContent, content.reactToastOptions)
            case ToastLevel.Warning => toast.warn(content.toastContent, content.reactToastOptions)
            case ToastLevel.Error   => toast.error(content.toastContent, content.reactToastOptions)
            case ToastLevel.Success => toast.success(content.toastContent, content.reactToastOptions)
            case ToastLevel.Dark    => toast.dark(content.toastContent, content.reactToastOptions)
          }
        }
      )
    )

  override def componentWillUnmount(): Unit = state.foreach(_.kill())

  def render(): ReactElement = div()
}
