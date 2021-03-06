package frontend.components.connected.menugames

import com.raquo.airstream.eventbus.EventBus
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import models.bff.ingame.Controls
import org.scalajs.dom.html
import programs.frontend.menus.controls._
import zio.ZIO
import com.raquo.laminar.api.L._
import frontend.components.utils.modal.UnderModalLayer
import frontend.components.utils.tailwind.modal.modalContainer
import frontend.components.utils.tailwind.forms._
import utils.laminarzio.onMountZIO
import frontend.components.utils.tailwind._
import models.bff.ingame.Controls.InputCode
import org.scalajs.dom
import typings.std.{KeyboardEvent, MouseEvent}
import utils.laminarzio.Implicits._
import utils.domutils.Implicits._
import services.toaster.toast

import scala.scalajs.js
import gamelogic.gameextras.GameMarker

final class GameControlsOptions private (closeWriter: Observer[Unit]) extends Component[html.Element] {

  val controlsBus: EventBus[Controls] = new EventBus

  val feedCurrentControl =
    retrieveControls.flatMap(controls => ZIO.effectTotal(controlsBus.writer.onNext(controls)))

  case class AssigningInfo(
      name: String,
      currentCode: InputCode,
      assign: (Controls, InputCode) => Controls,
      currentControls: Controls
  )

  /** Feed here Some an instance of [[AssigningInfo]] to modify a key, and None when the key has been modified. */
  val assigningKeyBus: EventBus[Option[AssigningInfo]] = new EventBus
  val assigningWindowVisible                           = assigningKeyBus.events.map(_.isDefined)

  val waitForAssignWindow = child <-- assigningKeyBus.events.map {
    case None => emptyNode
    case Some(assignInfo) =>
      val assignKeyboardCallback: js.Function1[KeyboardEvent, Unit] = (event: KeyboardEvent) => {
        val effect = storeControls(assignInfo.assign(assignInfo.currentControls, event.toInputCode)) *>
          feedCurrentControl

        utils.runtime.unsafeRunToFuture(effect)
        event.preventDefault()
        event.stopPropagation()
        assigningKeyBus.writer.onNext(None)
      }

      val assignMouseCallback: js.Function1[MouseEvent, Unit] = { (event: MouseEvent) =>
        if (event.button != 0) {

          val effect = storeControls(
            assignInfo.assign(assignInfo.currentControls, Controls.MouseCode(event.button.toInt))
          ) *>
            feedCurrentControl

          utils.runtime.unsafeRunToFuture(effect)
          event.preventDefault()
          event.stopPropagation()
          assigningKeyBus.writer.onNext(None)
        }
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
        onClick.preventDefault.stopPropagation.mapTo(Option.empty[AssigningInfo]) --> assigningKeyBus.writer,
        div(
          modalContainer,
          s"Press any key or any (non-left) mouse button to assign ${assignInfo.name}",
          onClick.preventDefault.stopPropagation --> Observer.empty
        ),
        onMountCallback { _ =>
          dom.window.addEventListener("keypress", assignKeyboardCallback)
          dom.window.addEventListener("mousedown", assignMouseCallback)
        },
        onUnmountCallback { _ =>
          dom.window.removeEventListener("keypress", assignKeyboardCallback)
          dom.window.removeEventListener("mousedown", assignMouseCallback)
        }
      )
  }

  private def controlSetting(name: String, code: InputCode, assign: (Controls, InputCode) => Controls)(
      implicit controls: Controls
  ) =
    div(
      formGroup,
      formLabel(name, (1, 2)),
      //div(className := "md:w-1/3"),
      div(
        className := "md:w-1/2",
        label(
          className := "bg-gray-400 border-indigo-800 p-2 rounded cursor-pointer",
          code.label,
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
    controlsBus.events
      .map(_.allKeysInMultiple)
      .filter(_.nonEmpty)
      .map(_.map(_.code))
      .flatMap(
        inputCodes => toast.warn(s"The following inputs are set twice: ${inputCodes.mkString(", ")}")
      ) --> Observer.empty,
    div(
      modalContainer,
      h1(h1_primary, "Game Controls"),
      h2(h2_primary, "Click on a code to reassign it"),
      onClick.stopPropagation.mapTo(()) --> Observer.empty,
      div(
        overflowY := "scroll",
        height := "500px",
        className := "p-5 grid grid-cols-2",
        div(
          className := "col-start-1 col-end-1",
          children <-- controlsBus.events.map { implicit controls =>
            List(
              controlSetting("Up", controls.upKey, (cs, c) => cs.copy(upKey                          = c)),
              controlSetting("Down", controls.downKey, (cs, c) => cs.copy(downKey                    = c)),
              controlSetting("Left", controls.leftKey, (cs, c) => cs.copy(leftKey                    = c)),
              controlSetting("Right", controls.rightKey, (cs, c) => cs.copy(rightKey                 = c)),
              controlSetting("Next target", controls.nextTargetKey, (cs, c) => cs.copy(nextTargetKey = c)),
              controlSetting(
                "Cross on target",
                controls.gameMarkerControls.crossTargetKey,
                (cs, c) => cs.copy(gameMarkerControls = cs.gameMarkerControls.copy(crossTargetKey = c))
              ),
              controlSetting(
                "Lozenge on target",
                controls.gameMarkerControls.lozengeTargetKey,
                (cs, c) => cs.copy(gameMarkerControls = cs.gameMarkerControls.copy(lozengeTargetKey = c))
              ),
              controlSetting(
                "Moon on target",
                controls.gameMarkerControls.moonTargetKey,
                (cs, c) => cs.copy(gameMarkerControls = cs.gameMarkerControls.copy(moonTargetKey = c))
              ),
              controlSetting(
                "Square on target",
                controls.gameMarkerControls.squareTargetKey,
                (cs, c) => cs.copy(gameMarkerControls = cs.gameMarkerControls.copy(squareTargetKey = c))
              ),
              controlSetting(
                "Star on target",
                controls.gameMarkerControls.starTargetKey,
                (cs, c) => cs.copy(gameMarkerControls = cs.gameMarkerControls.copy(starTargetKey = c))
              ),
              controlSetting(
                "Triangle on target",
                controls.gameMarkerControls.triangleTargetKey,
                (cs, c) => cs.copy(gameMarkerControls = cs.gameMarkerControls.copy(triangleTargetKey = c))
              ),
              controlSetting(
                "Fixed Cross",
                controls.gameMarkerControls.crossFixedKey,
                (cs, c) => cs.copy(gameMarkerControls = cs.gameMarkerControls.copy(crossFixedKey = c))
              ),
              controlSetting(
                "Fixed Lozenge",
                controls.gameMarkerControls.lozengeFixedKey,
                (cs, c) => cs.copy(gameMarkerControls = cs.gameMarkerControls.copy(lozengeFixedKey = c))
              ),
              controlSetting(
                "Fixed Moon",
                controls.gameMarkerControls.moonFixedKey,
                (cs, c) => cs.copy(gameMarkerControls = cs.gameMarkerControls.copy(moonFixedKey = c))
              ),
              controlSetting(
                "Fixed Square",
                controls.gameMarkerControls.squareFixedKey,
                (cs, c) => cs.copy(gameMarkerControls = cs.gameMarkerControls.copy(squareFixedKey = c))
              ),
              controlSetting(
                "Fixed Star",
                controls.gameMarkerControls.starFixedKey,
                (cs, c) => cs.copy(gameMarkerControls = cs.gameMarkerControls.copy(starFixedKey = c))
              ),
              controlSetting(
                "Fixed Triangle",
                controls.gameMarkerControls.triangleFixedKey,
                (cs, c) => cs.copy(gameMarkerControls = cs.gameMarkerControls.copy(triangleFixedKey = c))
              )
            )
          }
        ),
        div(
          className := "col-start-2 col-end-2",
          children <-- controlsBus.events.map { implicit controls =>
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
      button(btn, secondaryButton, "Restore defaults", onClick.mapTo(()) --> ((_: Unit) => {
        utils.runtime.unsafeRunToFuture(
          resetControls *> feedCurrentControl
        )
      }))
    ),
    onClick.mapTo(()) --> UnderModalLayer.closeModalWriter,
    onClick.mapTo(()) --> closeWriter,
    onMountZIO(feedCurrentControl),
    waitForAssignWindow
  )
}

object GameControlsOptions {

  def apply(closeWriter: Observer[Unit]): GameControlsOptions = new GameControlsOptions(closeWriter)

}
