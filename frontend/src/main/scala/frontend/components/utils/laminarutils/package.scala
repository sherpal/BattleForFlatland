package frontend.components.utils

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import slinky.core.KeyAndRefAddingStage
import slinky.web.ReactDOM
import org.scalajs.dom.html
import slinky.core.facade.ReactInstance

package object laminarutils {

  // (implicit constructorTag: ConstructorTag[Def]): KeyAndRefAddingStage[Def]
  def reactChild[Def, El <: Element](
      element: KeyAndRefAddingStage[Def],
      container: ReactiveHtmlElement[html.Element]
  ): Modifier[El] =
    new Modifier[El] {

      private val instance: Var[Option[ReactInstance]] = Var(Option.empty)

      private val mount = onMountCallback[El] { _ =>
        instance.update { _ =>
          Some(ReactDOM.render(element, container.ref))
        }
      }

      private val unmount = onUnmountCallback[El] { _ =>
        instance.now.foreach(_ => ReactDOM.unmountComponentAtNode(container.ref))
      }

      override def apply(element: El): Unit = {
        mount.apply(element)
        unmount.apply(element)
        container.apply(element)
      }
    }

}
