package components.beforegame

import com.raquo.laminar.api.L.*
import models.bff.ingame.Controls
import models.syntax.Pointed
import programs.frontend.menus.controls.*
import be.doeraene.webcomponents.ui5.*
import utils.laminarzio.*
import zio.*
import services.FrontendEnv
import be.doeraene.webcomponents.ui5.configkeys.TableMode
import models.bff.ingame.Controls.InputCode
import models.bff.ingame.Controls.KeyCode
import models.bff.ingame.Controls.ModifiedKeyCode
import models.bff.ingame.Controls.MouseCode
import scala.scalajs.js
import org.scalajs.dom
import models.bff.ingame.Controls.KeyInputModifier

object ControlSettings {

  def apply(resetEvents: EventStream[Unit])(using Runtime[FrontendEnv]): HtmlElement = {
    val currentSettings = Var(Pointed[Controls].unit)

    val keyBinderClickBus = new EventBus[(String, InputCode => Controls)]

    val currentModifyingSignal = EventStream
      .merge(
        keyBinderClickBus.events.mapTo(true),
        currentSettings.signal.changes.mapTo(false)
      )
      .startWith(false)

    def displayInputCode(
        description: String,
        code: InputCode,
        changeControls: InputCode => Controls
    ) =
      TableCompat.row(
        _.cell(description),
        _.cell(
          kbd(
            className := "key-binder",
            padding   := "0.5em",
            code match
              case KeyCode(code)                   => code
              case ModifiedKeyCode(code, modifier) => s"${modifier.name} $code"
              case MouseCode(code)                 => s"Button $code"
            ,
            onClick.mapTo((description, changeControls)) --> keyBinderClickBus.writer
          )
        )
      )

    div(
      currentSettings.signal.changes.flatMapSwitchZIO(controls =>
        storeControls(controls).orDie
      ) --> Observer.empty,
      styleTag("""
      |kbd.key-binder {
      |  background-color: #eee;
      |  border-radius: 3px;
      |  border: 1px solid #b4b4b4;
      |  box-shadow:
      |    0 1px 1px rgba(0, 0, 0, 0.2),
      |    0 2px 0 0 rgba(255, 255, 255, 0.7) inset;
      |  color: #333;
      |  display: inline-block;
      |  font-size: 0.85em;
      |  font-weight: 700;
      |  line-height: 1;
      |  padding: 2px 4px;
      |  white-space: nowrap;
      |  cursor: pointer;
      |}""".stripMargin),
      minWidth  := "500px",
      maxHeight := "600px",
      overflowY.auto,
      child.maybe <-- currentSettings.signal.combineWith(currentModifyingSignal).map {
        (controls, isModifying) =>
          Option.unless(isModifying)(
            TableCompat(
              _.mode          := TableMode.None,
              _.slots.columns := TableCompat.column("Action"),
              _.slots.columns := TableCompat.column("Key"),
              displayInputCode("Go Up", controls.upKey, code => controls.copy(upKey = code)),
              displayInputCode("Go Down", controls.downKey, code => controls.copy(downKey = code)),
              displayInputCode(
                "Go Right",
                controls.rightKey,
                code => controls.copy(rightKey = code)
              ),
              displayInputCode("Go Left", controls.leftKey, code => controls.copy(leftKey = code)),
              displayInputCode(
                "Next Target",
                controls.nextTargetKey,
                code => controls.copy(nextTargetKey = code)
              ),
              displayInputCode(
                "Toggle Target Lock-in",
                controls.targetLockInToggleKey,
                code => controls.copy(targetLockInToggleKey = code)
              ),
              controls.abilityKeys.zipWithIndex.map { (currentCode, index) =>
                displayInputCode(
                  s"Ability ${index + 1}",
                  currentCode,
                  code =>
                    controls.copy(abilityKeys = controls.abilityKeys.patch(index, List(code), 1))
                )
              },
              displayInputCode(
                "Cross On Target",
                controls.gameMarkerControls.crossTargetKey,
                code =>
                  controls.copy(gameMarkerControls =
                    controls.gameMarkerControls.copy(crossTargetKey = code)
                  )
              ),
              displayInputCode(
                "Cross On Ground",
                controls.gameMarkerControls.crossFixedKey,
                code =>
                  controls.copy(gameMarkerControls =
                    controls.gameMarkerControls.copy(crossFixedKey = code)
                  )
              ),
              displayInputCode(
                "Lozenge On Target",
                controls.gameMarkerControls.lozengeTargetKey,
                code =>
                  controls.copy(gameMarkerControls =
                    controls.gameMarkerControls.copy(lozengeTargetKey = code)
                  )
              ),
              displayInputCode(
                "Lozenge On Ground",
                controls.gameMarkerControls.lozengeFixedKey,
                code =>
                  controls.copy(gameMarkerControls =
                    controls.gameMarkerControls.copy(lozengeFixedKey = code)
                  )
              ),
              displayInputCode(
                "Moon On Target",
                controls.gameMarkerControls.moonTargetKey,
                code =>
                  controls.copy(gameMarkerControls =
                    controls.gameMarkerControls.copy(moonTargetKey = code)
                  )
              ),
              displayInputCode(
                "Moon on Ground",
                controls.gameMarkerControls.moonFixedKey,
                code =>
                  controls.copy(gameMarkerControls =
                    controls.gameMarkerControls.copy(moonFixedKey = code)
                  )
              ),
              displayInputCode(
                "Square On Target",
                controls.gameMarkerControls.squareTargetKey,
                code =>
                  controls.copy(gameMarkerControls =
                    controls.gameMarkerControls.copy(squareTargetKey = code)
                  )
              ),
              displayInputCode(
                "Square on Ground",
                controls.gameMarkerControls.squareFixedKey,
                code =>
                  controls.copy(gameMarkerControls =
                    controls.gameMarkerControls.copy(squareFixedKey = code)
                  )
              ),
              displayInputCode(
                "Star On Target",
                controls.gameMarkerControls.starTargetKey,
                code =>
                  controls.copy(gameMarkerControls =
                    controls.gameMarkerControls.copy(starTargetKey = code)
                  )
              ),
              displayInputCode(
                "Star on Ground",
                controls.gameMarkerControls.starFixedKey,
                code =>
                  controls.copy(gameMarkerControls =
                    controls.gameMarkerControls.copy(starFixedKey = code)
                  )
              ),
              displayInputCode(
                "Triangle On Target",
                controls.gameMarkerControls.triangleTargetKey,
                code =>
                  controls.copy(gameMarkerControls =
                    controls.gameMarkerControls.copy(triangleTargetKey = code)
                  )
              ),
              displayInputCode(
                "Triangle On Ground",
                controls.gameMarkerControls.triangleFixedKey,
                code =>
                  controls.copy(gameMarkerControls =
                    controls.gameMarkerControls.copy(triangleFixedKey = code)
                  )
              )
            )
          )
      },
      child.maybe <-- keyBinderClickBus.events
        .startWith(("", (_: InputCode) => Pointed[Controls].unit))
        .combineWith(currentModifyingSignal)
        .map((description, controlsModifier, isModifying) =>
          Option.when(isModifying)(
            div(
              height := "300px",
              display.flex,
              alignItems.center,
              alignContent.center,
              s"Please hit a key or mouse button to set up $description",
              onMountUnmountCallbackWithState(
                { _ =>
                  val keyupHandler: js.Function1[dom.KeyboardEvent, Unit] = { event =>
                    dom.console.log(event)
                    val code =
                      if event.code.startsWith("Key") then s"Key${event.key.toUpperCase()}"
                      else event.code
                    val inputCode =
                      if event.shiftKey then ModifiedKeyCode(code, KeyInputModifier.WithShift)
                      else KeyCode(code)
                    currentSettings.set(controlsModifier(inputCode))
                  }

                  val clickHandler: js.Function1[dom.MouseEvent, Unit] = { event =>
                    event.preventDefault()

                    dom.console.log(event)

                    val inputCode = MouseCode(event.button)
                    currentSettings.set(controlsModifier(inputCode))
                  }

                  val handlers = Vector("keyup" -> keyupHandler, "click" -> clickHandler)

                  js.timers.setTimeout(0)(
                    handlers.foreach((eventName, handler) =>
                      dom.window.addEventListener(eventName, handler)
                    )
                  )

                  handlers
                },
                (_, maybeHandler) =>
                  maybeHandler.foreach(
                    _.foreach((eventName, handler) =>
                      dom.window.removeEventListener(eventName, handler)
                    )
                  )
              )
            )
          )
        ),
      onMountZIO(for {
        controls <- retrieveControls
        _        <- ZIO.succeed(currentSettings.set(controls))
      } yield ()),
      resetEvents.flatMapSwitchZIO(_ =>
        storeControls(Pointed[Controls].unit).orDie
      ) --> currentSettings.writer
    )
  }

}
