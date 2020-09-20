package frontend.components.connected.menugames

import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import models.bff.ingame.KeyboardControls
import models.syntax.Pointed
import org.scalajs.dom.html
import services.localstorage._
import io.circe.generic.auto._
import zio.clock.Clock
import zio.{UIO, ZIO}

final class GameControlsOptions private () extends Component[html.Element] {

  private val layer = zio.clock.Clock.live >>> services.localstorage.FLocalStorage.live

  val storageKey = "controls"

  val retrieveKeyboardControls: UIO[KeyboardControls] = (for {
    maybeFromLocalStorage <- retrieveFrom[KeyboardControls](storageKey)
      .catchAll(_ => ZIO.some(Pointed[KeyboardControls].unit))
    keyboardControls = maybeFromLocalStorage.getOrElse(Pointed[KeyboardControls].unit)
  } yield keyboardControls).provideLayer(layer)

  def storeKeyboardControls(keyboardControls: KeyboardControls) =
    storeAt(storageKey, keyboardControls).provideLayer(layer)

  val element: ReactiveHtmlElement[html.Element] = ???
}

object GameControlsOptions {}
