package frontend.components.connected.menugames

import com.raquo.airstream.eventbus.EventBus
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import models.bff.ingame.KeyboardControls
import org.scalajs.dom.html
import programs.frontend.menus.controls._
import zio.ZIO
import com.raquo.laminar.api.L._
import frontend.components.utils.modal.UnderModalLayer
import frontend.components.utils.tailwind.modal.modalContainer
import frontend.components.utils.tailwind.forms._
import utils.laminarzio.onMountZIO
import frontend.components.utils.tailwind._
import models.bff.ingame.KeyboardControls.KeyCode
import org.scalajs.dom
import typings.std.KeyboardEvent

import scala.scalajs.js

final class GameControlsOptions private (closeWriter: Observer[Unit]) extends Component[html.Element] {

  val keyboardControlsBus: EventBus[KeyboardControls] = new EventBus

  case class AssigningInfo(
      name: String,
      currentCode: KeyCode,
      assign: (KeyboardControls, KeyCode) => KeyboardControls,
      currentControls: KeyboardControls
  )

  /** Feed here Some an instance of [[AssigningInfo]] to modify a key, and None when the key has been modified. */
  val assigningKeyBus: EventBus[Option[AssigningInfo]] = new EventBus
  val assigningWindowVisible                           = assigningKeyBus.events.map(_.isDefined)

  val waitForAssignWindow = child <-- assigningKeyBus.events.map {
    case None => emptyNode
    case Some(assignInfo) =>
      val assignCallback: js.Function1[KeyboardEvent, Unit] = (event: KeyboardEvent) => {
        val effect = storeKeyboardControls(assignInfo.assign(assignInfo.currentControls, event.code)) *>
          retrieveKeyboardControls.flatMap(controls => ZIO.effectTotal(keyboardControlsBus.writer.onNext(controls)))

        utils.runtime.unsafeRunToFuture(effect)
        event.preventDefault()
        event.stopPropagation()
        assigningKeyBus.writer.onNext(None)
      }

      div(
        zIndex := 7,
        position := "fixed",
        top := "0",
        left := "0",
        width := "100%",
        height := "100%",
        className := "flex flex-col items-center justify-center",
        className := "bg-opacity-25 bg-indigo-900",
        onClick.preventDefault.mapTo(Option.empty[AssigningInfo]) --> assigningKeyBus.writer,
        div(
          modalContainer,
          s"Press any key to assign ${assignInfo.name}",
          onClick.preventDefault --> Observer.empty
        ),
        onMountCallback { _ =>
          dom.window.addEventListener("keypress", assignCallback)
        },
        onUnmountCallback { _ =>
          dom.window.removeEventListener("keypress", assignCallback)
        }
      )
  }

  private def controlSetting(name: String, code: KeyCode, assign: (KeyboardControls, KeyCode) => KeyboardControls)(
      implicit controls: KeyboardControls
  ) =
    div(
      formGroup,
      formLabel(name),
      div(className := "md:w-1/3"),
      div(
        className := "md:w-1/3",
        label(
          className := "bg-gray-400 border-indigo-800 p-2 rounded cursor-pointer",
          code,
          onClick.mapTo(Some(AssigningInfo(name, code, assign, controls))) --> assigningKeyBus.writer
        )
      )
    )

  val element: ReactiveHtmlElement[html.Element] = div(
    zIndex := 6,
    position := "fixed",
    left := "0",
    top := "0",
    width := "100%",
    height := "100%",
    className := "flex flex-col items-center justify-center",
    div(
      modalContainer,
      height := "600px",
      overflowY := "scroll",
      h1(h1_primary, "Game Controls"),
      h2(h2_primary, "Click on a code to reassign it"),
      onClick.stopPropagation.mapTo(()) --> Observer.empty,
      div(
        className := "p-5 grid grid-cols-3",
        div(
          className := "col-start-1 col-end-1",
          children <-- keyboardControlsBus.events.map { implicit controls =>
            List(
              controlSetting("Up", controls.upKey, (cs, c) => cs.copy(upKey          = c)),
              controlSetting("Down", controls.downKey, (cs, c) => cs.copy(downKey    = c)),
              controlSetting("Left", controls.leftKey, (cs, c) => cs.copy(leftKey    = c)),
              controlSetting("Right", controls.rightKey, (cs, c) => cs.copy(rightKey = c))
            )
          }
        ),
        div(
          className := "col-start-3 col-end-3",
          children <-- keyboardControlsBus.events.map { implicit controls =>
            controls.abilityKeys.zipWithIndex.map {
              case (code, idx) =>
                controlSetting(
                  s"Ability ${idx + 1}",
                  code,
                  (cs, c) => cs.copy(abilityKeys = cs.abilityKeys.patch(idx, List(c), 1))
                )
            }
          }
        )
      ),
      "todo: restore defaults"
    ),
    onClick.mapTo(()) --> UnderModalLayer.closeModalWriter,
    onClick.mapTo(()) --> closeWriter,
    onMountZIO(
      retrieveKeyboardControls.flatMap(controls => ZIO.effectTotal(keyboardControlsBus.writer.onNext(controls)))
    ),
    waitForAssignWindow
  )
}

object GameControlsOptions {

  def apply(closeWriter: Observer[Unit]): GameControlsOptions = new GameControlsOptions(closeWriter)

}
