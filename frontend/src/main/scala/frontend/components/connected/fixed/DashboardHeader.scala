package frontend.components.connected.fixed

import com.raquo.airstream.eventstream.EventStream
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import frontend.components.utils.Logout
import org.scalajs.dom.html
import frontend.components.utils.tailwind._

final class DashboardHeader private ($userName: EventStream[String]) extends Component[html.Element] {
  val element: ReactiveHtmlElement[html.Element] = header(
    headerStyle,
    h1(h(8), pad(2), "Battle for Flatland"),
    ul(
      className := "flex justify-between",
      pad(2),
      li(
        child <-- $userName.startWith("...").map(
          span(
            _,
            pad(2)
          )
        )
      ),
      li(span(pad(2), className := "cursor-pointer hover:text-indigo-200", Logout()))
    )
  )
}

object DashboardHeader {
  def apply($userName: EventStream[String]) = new DashboardHeader($userName)
}
