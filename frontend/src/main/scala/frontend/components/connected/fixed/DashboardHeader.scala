package frontend.components.connected.fixed

import com.raquo.airstream.eventstream.EventStream
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import frontend.components.utils.Logout
import frontend.components.utils.tailwind._
import org.scalajs.dom.html

final class DashboardHeader private ($userName: EventStream[String]) extends Component[html.Element] {
  val element: ReactiveHtmlElement[html.Element] = header(
    headerStyle,
    h1(className := "text-3xl", pad(2), globals.projectName),
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
      li(span(pad(2), className := s"cursor-pointer hover:text-$primaryColour-$primaryColourVeryLight", Logout()))
    )
  )
}

object DashboardHeader {
  def apply($userName: EventStream[String]) = new DashboardHeader($userName)
}
