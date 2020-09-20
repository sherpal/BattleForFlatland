package frontend.components.utils

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html
import programs.frontend.login.logout
import utils.laminarzio.Implicits._

object Logout {

  private val program = logout

  def apply(): ReactiveHtmlElement[html.Span] = span(
    onClick --> (_ => EventStream.fromZIOEffect(program)),
    "Logout"
  )

}
