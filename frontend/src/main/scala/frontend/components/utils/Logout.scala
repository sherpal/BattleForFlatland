package frontend.components.utils

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html
import programs.frontend.login.logout
import services.http.FrontendHttpClient
import services.routing.FRouting
import utils.laminarzio.Implicits._

object Logout {

  private val layer = FRouting.live ++ FrontendHttpClient.live

  def apply(): ReactiveHtmlElement[html.Span] = span(
    onClick --> (_ => EventStream.fromZIOEffect(logout.provideLayer(layer))),
    "Logout"
  )

}
