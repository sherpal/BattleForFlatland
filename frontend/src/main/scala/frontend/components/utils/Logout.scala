package frontend.components.utils

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import errors.ErrorADT
import org.scalajs.dom.html
import programs.frontend.login.logout
import services.http.FrontendHttpClient
import services.routing.FRouting
import utils.laminarzio.Implicits._
import zio.IO

object Logout {

  private val layer                       = FRouting.live ++ FrontendHttpClient.live
  private val program: IO[ErrorADT, Unit] = logout.provideLayer(layer)

  def apply(): ReactiveHtmlElement[html.Span] = span(
    onClick --> (_ => EventStream.fromZIOEffect(program)),
    "Logout"
  )

}
