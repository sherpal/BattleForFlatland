package frontend.components.utils.bootstrap

import java.util.UUID

import org.scalajs.dom
import org.scalajs.dom.html
import scalatags.JsDom
import typings.popperjsCore.{mod => Popper}
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.{ReactiveElement, ReactiveHtmlElement}

object PopperElement {

  //    div(
  //      className := "popover fade bs-popover show",
  //      role := "tooltip",
  //      styleAttr := "position: absolute; will-change: transform; top: 0px; left: 0px; transform: translate3d(250px, 500px, 0px);",
  //      div(
  //        className := "arrow",
  //        top := "40px"
  //      ),
  //      h3(className := "popover-header", "Popover title"),
  //      div(className := "popover-body", "un gros body de la mort qui tue")
  //    )

  private object Apply {
    import scalatags.JsDom.all._

    def apply(title: String, body: String): JsDom.TypedTag[dom.html.Div] =
      div(
        cls := "popover fade bs-popover show",
        role := "tooltip",
        h3(cls := "popover-header", title),
        div(cls := "popover-body", body)
      )
  }

  def createPopover(attachedTo: html.Element): Unit = {

    val describedBy = "aria-described-by"

    Option(attachedTo.getAttribute(describedBy)) match {
      case Some(id) =>
        attachedTo.removeAttribute(describedBy)
        val popover = dom.document.getElementById(id)
        popover.parentNode.removeChild(popover)
      case None =>
        dom.console.log(attachedTo.dataset)
        val title          = attachedTo.dataset.apply("originalTitle")
        val body           = attachedTo.dataset.apply("content")
        val tooltipElement = Apply(title, body).render

        tooltipElement.id = "popover-" + UUID.randomUUID().toString

        attachedTo.setAttribute(describedBy, tooltipElement.id)

        dom.document.body.appendChild(tooltipElement)

        Popper.createPopper(
          attachedTo.asInstanceOf[typings.std.HTMLElement],
          tooltipElement.asInstanceOf[typings.std.HTMLElement]
        )

    }

  }

  def attachedPopover[El <: ReactiveElement[dom.html.Element]]: Modifier[El] =
    inContext[El](elem => onClick.mapTo(elem.ref) --> (ref => createPopover(ref)))

}
