package frontend.components.test

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.utils.modal.UnderModalLayer
import org.scalajs.dom.html

object Test {

  def apply(): ReactiveHtmlElement[html.Div] = div(
    button("click", onClick.mapTo(()) --> UnderModalLayer.showModalWriter),
    child <-- UnderModalLayer.closeModalEvents.mapTo("closed!")
  )

}
