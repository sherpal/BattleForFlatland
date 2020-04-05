package frontend.components

import com.raquo.laminar.lifecycle.{NodeDidMount, NodeWasDiscarded, NodeWillUnmount}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom

import scala.language.implicitConversions

trait LifecycleComponent[Ref <: dom.html.Element] extends Component[Ref] {

  val elem: ReactiveHtmlElement[Ref]

  def componentDidMount(): Unit     = {}
  def componentWillUnmount(): Unit  = {}
  def componentWasDiscarded(): Unit = {}

  final lazy val element: ReactiveHtmlElement[Ref] = {
    elem.subscribe(_.mountEvents) {
      case NodeDidMount     => componentDidMount()
      case NodeWillUnmount  => componentWillUnmount()
      case NodeWasDiscarded => componentWasDiscarded()
    }

    elem
  }

}

object LifecycleComponent {

  implicit def asElement[Ref <: dom.html.Element](component: LifecycleComponent[Ref]): ReactiveHtmlElement[Ref] =
    Component.asElement(component)

}
