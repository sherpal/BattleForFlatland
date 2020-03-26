package frontend.router

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html
import urldsl.language.PathSegment

object Link {

  def apply(to: PathSegment[Unit, _])(text: String): ReactiveHtmlElement[html.Span] =
    span(className := "clickable", onClick --> (_ => Router.router.moveTo("/" + to.createPath())), text)

}
