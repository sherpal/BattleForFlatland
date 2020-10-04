package frontend.components.utils.toasts

import com.raquo.airstream.eventstream.EventStream
import slinky.core.StatelessComponent
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html._

@react final class ToasterWrapper() extends StatelessComponent {
  type Props = Unit

  def render(): ReactElement = div(
    typings.reactToastify.components.ToastContainer(),
    Toaster()
  )
}
