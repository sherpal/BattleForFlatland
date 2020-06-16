package frontend.components.utils.loading

import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import org.scalajs.dom.html
import org.scalajs.dom.html.Div
import com.raquo.laminar.api.L._

final class LoadingScreen private () extends Component[html.Div] {

  val element: ReactiveHtmlElement[Div] =
    div(
      className := "absolute top-0 left-0 w-full h-full flex items-center justify-center",
      backgroundColor := "rgba(128,128,128,0.5)",
      div(
        className := "loader"
      )
    )
}

object LoadingScreen {
  def apply(): LoadingScreen = new LoadingScreen
}
