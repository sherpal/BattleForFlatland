package bffclient.controls

import bffclient.gui.*
import bffclient.love.LoveInput
import bffclient.ui.{ButtonStyle, Theme}
import models.bff.ingame.{Controls, GameMarkerControls}
import models.bff.ingame.Controls.InputCode
import models.syntax.Pointed

/** Changes the settings, in two tabs:
  *   - the controls, like the frontend's `ControlSettings`: click a binding, then press the key
  *     (possibly with Shift) or the mouse button to bind to it. Every change is saved to
  *     `controls.json` (see [[ControlsFile]]), which the next game loads;
  *   - the client's own settings ([[ClientSettings]]), saved to `settings.json`.
  *
  * A dialog over the menus (which stay open behind, e.g. the lobby and its websocket): the frames
  * under it cannot be clicked. Its frame is removed when it is closed.
  */
final class SettingsEditor(parent: Frame, close: () => Unit) {
  private var controls = ControlsFile.load()._1
  private var settings = ClientSettings.current
  private var error    = Option.empty[String]

  private enum Tab(val label: String) {
    case Bindings extends Tab("Controls")
    case General extends Tab("General")
  }
  private var tab = Tab.Bindings

  /** The action whose binding is being changed: its column and its index there. */
  private var waiting = Option.empty[(Controls => Seq[Binding], Int)]

  private final case class Binding(action: String, code: InputCode, change: InputCode => Controls)

  private def movementsAndAbilities(c: Controls): Seq[Binding] = Seq(
    Binding("Go up", c.upKey, code => c.copy(upKey = code)),
    Binding("Go down", c.downKey, code => c.copy(downKey = code)),
    Binding("Go left", c.leftKey, code => c.copy(leftKey = code)),
    Binding("Go right", c.rightKey, code => c.copy(rightKey = code)),
    Binding("Next target", c.nextTargetKey, code => c.copy(nextTargetKey = code)),
    Binding(
      "Face the target",
      c.targetLockInToggleKey,
      code => c.copy(targetLockInToggleKey = code)
    )
  ) ++ c.abilityKeys.zipWithIndex.map((key, i) =>
    Binding(s"Ability ${i + 1}", key, code => c.copy(abilityKeys = c.abilityKeys.updated(i, code)))
  )

  private def markers(c: Controls): Seq[Binding] = {
    val m                                                   = c.gameMarkerControls
    def change(f: GameMarkerControls => GameMarkerControls) = c.copy(gameMarkerControls = f(m))
    Seq(
      Binding("Cross on target", m.crossTargetKey, code => change(_.copy(crossTargetKey = code))),
      Binding("Cross on ground", m.crossFixedKey, code => change(_.copy(crossFixedKey = code))),
      Binding(
        "Lozenge on target",
        m.lozengeTargetKey,
        code => change(_.copy(lozengeTargetKey = code))
      ),
      Binding(
        "Lozenge on ground",
        m.lozengeFixedKey,
        code => change(_.copy(lozengeFixedKey = code))
      ),
      Binding("Moon on target", m.moonTargetKey, code => change(_.copy(moonTargetKey = code))),
      Binding("Moon on ground", m.moonFixedKey, code => change(_.copy(moonFixedKey = code))),
      Binding(
        "Square on target",
        m.squareTargetKey,
        code => change(_.copy(squareTargetKey = code))
      ),
      Binding("Square on ground", m.squareFixedKey, code => change(_.copy(squareFixedKey = code))),
      Binding("Star on target", m.starTargetKey, code => change(_.copy(starTargetKey = code))),
      Binding("Star on ground", m.starFixedKey, code => change(_.copy(starFixedKey = code))),
      Binding(
        "Triangle on target",
        m.triangleTargetKey,
        code => change(_.copy(triangleTargetKey = code))
      ),
      Binding(
        "Triangle on ground",
        m.triangleFixedKey,
        code => change(_.copy(triangleFixedKey = code))
      )
    )
  }

  private def set(newControls: Controls): Unit = {
    controls = newControls
    error = ControlsFile.save(newControls)
    render()
  }

  private def change(newSettings: ClientSettings): Unit = {
    settings = newSettings
    error = settings.save()
    render()
  }

  private def closeEditor(): Unit = {
    frame.remove()
    close()
  }

  // --- Frames ---

  val frame: Frame = Frame(parent)
  frame.setAllPoints()
  frame.setFrameStrata(Strata.Dialog)
  frame.setBackdrop(Backdrop(Theme.background.withAlpha(0.75)))
  frame.enableMouse(true) // nothing under it can be clicked
  frame.enableKeyboard(true)
  frame.gui.clearFocus()

  private val box = Templates.panel(frame)
  box.setSize(900, 600)
  box.setPoint(Point.Center)
  box.createFontString("Settings", Theme.large).setPoint(Point.TopLeft, 24, 18)

  private val tabs = Segmented[Tab](box, _.label)
  tabs.setSize(260, 32)
  tabs.setPoint(Point.TopRight, -24, 16)
  tabs.setScript(tabs.OnValueChanged) { chosen =>
    tab = chosen
    render()
  }

  private def view(): Frame = {
    val view = Frame(box)
    view.setAllPoints()
    view
  }

  // Controls
  private val bindingsView = view()
  bindingsView
    .createFontString(
      "Click a binding, then press a key (possibly with Shift) or a mouse button. Changes apply to the next game.",
      Theme.small,
      Theme.muted
    )
    .setPoint(Point.TopLeft, 24, 52)

  /** The buttons of the bindings, with what they show. */
  private val bindingButtons =
    for
      (column, left) <- Seq[(Controls => Seq[Binding], Double)](
        (movementsAndAbilities, 24),
        (markers, 470)
      )
      (binding, i) <- column(controls).zipWithIndex
    yield {
      val y = 84 + 25 * i
      bindingsView.createFontString(binding.action).setPoint(Point.TopLeft, left, y + 3)
      val button = Button(bindingsView)
      button.setSize(200, 23)
      button.setPoint(Point.TopLeft, left + 190, y)
      button.setScript(Script.OnClick) { _ =>
        waiting = Some((column, i))
        render()
      }
      button -> ((c: Controls) => column(c)(i).code.label)
    }

  private val toggleY   = 84 + 25 * markers(controls).size + 12
  private val mouseOver = CheckButton(bindingsView, "Mouse over")
  mouseOver.setPoint(Point.TopLeft, 470, toggleY)
  mouseOver.setScript(Script.OnClick)(_ =>
    set(controls.copy(mouseOverEnabled = mouseOver.isChecked))
  )
  private val mouseOverHelp = bindingsView.createFontString(
    "Abilities fall back to the frame under the mouse when the target is not valid for them.",
    Theme.small,
    Theme.muted
  )
  mouseOverHelp.setWordWrap(true)
  mouseOverHelp.setWidth(390)
  mouseOverHelp.setPoint(Point.TopLeft, 470, toggleY + 28)

  private val boundTwice = bindingsView.createFontString(colour = Theme.negative.lighter(0.3))
  boundTwice.setPoint(Point.TopLeft, 24, 552)
  private val resetButton = Button(bindingsView, "Reset to defaults")
  resetButton.setSize(190, 40)
  resetButton.setPoint(Point.TopLeft, 520, 540)
  resetButton.setScript(Script.OnClick)(_ => set(Pointed[Controls].unit))

  // Waiting for the key or mouse button to bind (see `capture`)
  private val captureView = view()
  private val captureText = captureView.createFontString(size = Theme.large)
  captureText.setPoint(Point.Center, box, Point.Top, 0, 265)
  captureView
    .createFontString("Escape to cancel", colour = Theme.muted)
    .setPoint(Point.Center, box, Point.Top, 0, 302)

  // The client's own settings (BFF's controls cannot hold them)
  private val generalView = view()
  private def help(text: String, y: Double): Unit = {
    val paragraph = generalView.createFontString(text, Theme.small, Theme.muted)
    paragraph.setWordWrap(true)
    paragraph.setWidth(560)
    paragraph.setPoint(Point.TopLeft, 24, y)
  }
  generalView.createFontString("HUD scale").setPoint(Point.TopLeft, 24, 88)
  private val hudScale = Selector[Double](generalView, scale => f"$scale%.1f")
  hudScale.setSize(200, 26)
  hudScale.setPoint(Point.TopLeft, 214, 84)
  hudScale.setScript(hudScale.OnValueChanged)(scale => change(settings.copy(hudScale = scale)))
  help(
    "The size of the frames and of the action bar, whatever the window's size. It applies to the next game.",
    118
  )
  private val hostGameServers = CheckButton(generalView, "Host game servers")
  hostGameServers.setPoint(Point.TopLeft, 24, 184)
  hostGameServers.setScript(Script.OnClick)(_ =>
    change(settings.copy(hostGameServers = hostGameServers.isChecked))
  )
  help(
    "The game server of a game runs on the computer of one of its players. To be ready to run it, " +
      "the client downloads it, with what runs it (several tens of MB, once), as soon as you join " +
      "a game. Turn this off to download nothing, e.g. on a mobile network: the other players " +
      "will run it.",
    216
  )

  // Footer
  private val footer = view()
  private val errorText =
    footer.createFontString(size = Theme.small, colour = Theme.negative.lighter(0.3))
  errorText.setPoint(Point.TopLeft, 24, 526)
  private val doneButton = Button(footer, "Done", ButtonStyle.Primary)
  doneButton.setSize(150, 40)
  doneButton.setPoint(Point.TopLeft, 726, 540)
  doneButton.setScript(Script.OnClick)(_ => closeEditor())

  frame.setScript(Script.OnKeyDown)(scancode =>
    waiting match {
      case Some(_) if scancode == "escape" => waiting = None; render()
      case Some((column, i)) =>
        if scancode != "lshift" && scancode != "rshift" then
          for code <- LoveInput.inputCode(scancode) do bind(column, i, code)
      case None => if scancode == "escape" then closeEditor()
    }
  )
  // while waiting, a click anywhere is the mouse button to bind
  frame.setScript(Script.OnMouseDown)(button =>
    for (column, i) <- waiting do bind(column, i, LoveInput.mouseCode(button))
  )

  render()

  private def bind(column: Controls => Seq[Binding], index: Int, code: InputCode): Unit = {
    waiting = None
    set(column(controls)(index).change(code))
  }

  /** Shows the settings in the frames. */
  private def render(): Unit = {
    tabs.setOptions(Tab.values.toSeq, Some(tab))
    tabs.setEnabled(waiting.isEmpty)
    bindingsView.setShown(tab == Tab.Bindings && waiting.isEmpty)
    captureView.setShown(tab == Tab.Bindings && waiting.isDefined)
    generalView.setShown(tab == Tab.General)
    footer.setShown(!(tab == Tab.Bindings && waiting.isDefined))

    for (column, i) <- waiting do
      captureText.setText(s"Press a key or a mouse button for \"${column(controls)(i).action}\"")
    for (button, label) <- bindingButtons do button.setText(label(controls))
    mouseOver.setChecked(controls.mouseOverEnabled)
    boundTwice.setText(
      controls.maybeMultipleKey.fold("")(key => s"${key.label} is bound to several actions")
    )

    hudScale.setOptions(ClientSettings.hudScales, Some(settings.hudScale))
    hostGameServers.setChecked(settings.hostGameServers)

    errorText.setText(error.getOrElse(""))
  }
}
