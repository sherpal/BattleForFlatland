package frontend.components.utils.popper

import java.util.UUID

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.{ReactiveElement, ReactiveHtmlElement}
import org.scalajs.dom
import org.scalajs.dom.html
import scalatags.JsDom
import typings.popperjsCore.anon.PartialOptions
import typings.popperjsCore.typesMod.Instance
import typings.popperjsCore.{mod => Popper}

import scala.language.implicitConversions

object PopperElement {

  def createPopper[ToAttachEl <: html.Element, El <: Element](
      toAttach: ReactiveHtmlElement[ToAttachEl],
      placementOption: typings.popperjsCore.enumsMod.Placement
  ): Modifier[El] = {

    val clickBus                              = new EventBus[Unit]
    var maybePopperInstance: Option[Instance] = Option.empty

    def showPopper(element: dom.Element): Unit = maybePopperInstance match {
      case Some(instance) =>
        maybePopperInstance = None
        instance.destroy()
      case None =>
        maybePopperInstance = Some(
          maybePopperInstance.getOrElse(
            Popper.createPopper(
              element,
              toAttach.ref,
            ???//PartialOptions().setPlacement(???)
            )
          )
        )
    }

    List(
      inContext[El](elem => onClick.mapTo(elem.ref) --> showPopper _),
      onClick.mapTo(()) --> clickBus,
      child <-- clickBus.events
        .fold(false) { (bool, _) =>
          !bool
        }
        .map(if (_) toAttach else emptyNode)
    )

  }

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

  private def createPopover(attachedTo: html.Element): Unit = {

    val describedBy = "aria-described-by"

    Option(attachedTo.getAttribute(describedBy)) match {
      case Some(id) =>
        attachedTo.removeAttribute(describedBy)
        val popover = dom.document.getElementById(id)
        popover.parentNode.removeChild(popover)
      case None =>
        val title          = attachedTo.dataset.apply("originalTitle")
        val body           = attachedTo.dataset.apply("content")
        val tooltipElement = Apply(title, body).render

        tooltipElement.id = "popover-" + UUID.randomUUID().toString

        attachedTo.setAttribute(describedBy, tooltipElement.id)

        dom.document.body.appendChild(tooltipElement)

        Popper.createPopper(
          attachedTo,
          tooltipElement
        )

    }

  }

  def attachPopover[El <: ReactiveElement[dom.html.Element]]: Modifier[El] =
    inContext[El](elem => onClick.mapTo(elem.ref) --> (ref => createPopover(ref)))

}
