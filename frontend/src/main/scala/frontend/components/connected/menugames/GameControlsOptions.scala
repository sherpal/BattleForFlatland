package frontend.components.connected.menugames

import com.raquo.airstream.eventbus.EventBus
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import models.bff.ingame.KeyboardControls
import org.scalajs.dom.html
import programs.frontend.menus.controls._
import zio.{UIO, ZIO}
import com.raquo.laminar.api.L._
import io.circe.syntax._
import io.circe.generic.auto._

final class GameControlsOptions private () extends Component[html.Element] {

  val keyboardControlsBus: EventBus[KeyboardControls] = new EventBus

  val element: ReactiveHtmlElement[html.Element] = div(
    zIndex := 6,
    position := "absolute",
    left := "0",
    top := "0",
    width := "100%",
    height := "100%",
    className := "flex flex-col items-center justify-center",
    child <-- keyboardControlsBus.events
      .map(_.asJson.spaces2)
      .map(
        pre(
          _,
          className := "bg-white"
        )
      )
  )
}

object GameControlsOptions {

  def apply(): GameControlsOptions = new GameControlsOptions()

}
