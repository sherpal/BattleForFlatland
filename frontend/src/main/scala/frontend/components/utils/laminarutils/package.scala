package frontend.components.utils

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import slinky.core.KeyAndRefAddingStage
import slinky.web.ReactDOM
import org.scalajs.dom.html
import org.scalajs.dom
import slinky.core.facade.ReactInstance

package object laminarutils {

  /**
    * Insert a Slinky React element as modifier of a Laminar [[com.raquo.laminar.nodes.ReactiveHtmlElement]].
    * The react element is mounted on the `onMountCallback` of the parent, and removed on the `onUnmountCallback`. It is
    * mounted inside the specified container. The container is then automatically added to the parent element.
    * @param element Slinky react element. The type is scary but it is the one you get when calling the apply method of
    *                a Slinky element.
    * @param container Laminar element in which to mount the React element. Should be empty. We leave it as an argument
    *                  so that it can be set to a specific one of your chosing.
    */
  def reactChild[Def, El <: Element](
      element: KeyAndRefAddingStage[Def],
      container: ReactiveHtmlElement[html.Element]
  ): Modifier[El] = {
    val instance: Var[Option[ReactInstance]] = Var(Option.empty)

    List(
      onMountCallback[El] { _ =>
        instance.update { _ =>
          Some(ReactDOM.render(element, container.ref))
        }
      },
      onUnmountCallback[El] { _ =>
        instance.now.foreach(_ => ReactDOM.unmountComponentAtNode(container.ref))
      },
      container
    )
  }

  /**
    * Same as reactChild, but the container is given for free as a div.
    */
  def reactChildInDiv[Def, El <: Element](element: KeyAndRefAddingStage[Def]): Modifier[El] =
    reactChild[Def, El](element, div())

}
