package frontend.components.utils

import urldsl.language.PathSegment
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html
import utils.laminarzio.Implicits._
import services.routing._

object Redirect {

  def apply(path: PathSegment[Unit, _]): ReactiveHtmlElement[html.Div] = div(
    child <-- EventStream.fromZIOEffect(moveTo(path).provideLayer(FRouting.live)).mapTo("Redirecting")
  )

}
