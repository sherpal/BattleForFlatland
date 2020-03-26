package frontend.components

import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom

import scala.language.implicitConversions

trait Component[Ref <: dom.html.Element] {

  val element: ReactiveHtmlElement[Ref]

}

object Component {

  implicit def asElement[Ref <: dom.html.Element](component: Component[Ref]): ReactiveHtmlElement[Ref] =
    component.element

}
