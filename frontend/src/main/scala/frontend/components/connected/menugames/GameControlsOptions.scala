package frontend.components.connected.menugames

import com.raquo.airstream.eventbus.EventBus
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import models.bff.ingame.KeyboardControls
import org.scalajs.dom.html
import programs.frontend.menus.controls._
import zio.{Exit, UIO, ZIO}
import com.raquo.laminar.api.L._
import frontend.components.utils.modal.UnderModalLayer
import io.circe.syntax._
import io.circe.generic.auto._
import org.scalajs.dom

final class GameControlsOptions private (closeWriter: Observer[Unit]) extends Component[html.Element] {

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
      ),
    onClick.mapTo(()) --> UnderModalLayer.closeModalWriter,
    onClick.mapTo(()) --> closeWriter,
    onMountCallback(
      _ =>
        utils.runtime.unsafeRunAsync(retrieveKeyboardControls) {
          case Exit.Success(value) => keyboardControlsBus.writer.onNext(value)
          case Exit.Failure(cause) => dom.console.error(cause.prettyPrint)
        }
    )
  )
}

object GameControlsOptions {

  def apply(closeWriter: Observer[Unit]): GameControlsOptions = new GameControlsOptions(closeWriter)

}
