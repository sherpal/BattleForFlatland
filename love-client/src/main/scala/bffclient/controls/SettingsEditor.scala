package bffclient.controls

import bffclient.gui.*
import bffclient.love.LoveInput
import bffclient.ui.{ButtonStyle, Theme}
import models.bff.ingame.{Controls, GameMarkerControls}
import models.bff.ingame.Controls.InputCode
import models.syntax.Pointed
import services.i18n.{I18n, Language}

/** Changes the settings, in two tabs:
  *   - the controls, like the frontend's `ControlSettings`: click a binding, then press the key
  *     (possibly with Shift) or the mouse button to bind to it. Every change is saved to
  *     `controls.json` (see [[ControlsFile]]), which the next game loads;
  *   - the client's own settings ([[ClientSettings]]), saved to `settings.json`.
  *
  * A dialog over the menus (which stay open behind, e.g. the lobby and its websocket): the frames
  * under it cannot be clicked. Its frame is removed when it is closed. Its texts are in the
  * language of `i18n`: when another language is chosen, it calls `languageChanged` (which may
  * replace it by one in that language, opened `onGeneral`).
  */
final class SettingsEditor(
    parent: Frame,
    i18n: I18n,
    close: () => Unit,
    languageChanged: () => Unit,
    onGeneral: Boolean = false
) {
  private var controls = ControlsFile.load()._1
  private var settings = ClientSettings.current
  private var error    = Option.empty[String]

  private enum Tab(val label: I18n => String) {
    case Bindings extends Tab(_.controls)
    case General extends Tab(_.general)
  }
  private var tab = if onGeneral then Tab.General else Tab.Bindings

  /** The action whose binding is being changed: its column and its index there. */
  private var waiting = Option.empty[(Controls => Seq[Binding], Int)]

  private final case class Binding(action: String, code: InputCode, change: InputCode => Controls)

  private def movementsAndAbilities(c: Controls): Seq[Binding] = Seq(
    Binding(i18n.goUp, c.upKey, code => c.copy(upKey = code)),
    Binding(i18n.goDown, c.downKey, code => c.copy(downKey = code)),
    Binding(i18n.goLeft, c.leftKey, code => c.copy(leftKey = code)),
    Binding(i18n.goRight, c.rightKey, code => c.copy(rightKey = code)),
    Binding(i18n.nextTarget, c.nextTargetKey, code => c.copy(nextTargetKey = code)),
    Binding(
      i18n.faceTheTarget,
      c.targetLockInToggleKey,
      code => c.copy(targetLockInToggleKey = code)
    )
  ) ++ c.abilityKeys.zipWithIndex.map((key, i) =>
    Binding(
      i18n.abilityNumber(i + 1),
      key,
      code => c.copy(abilityKeys = c.abilityKeys.updated(i, code))
    )
  )

  private def markers(c: Controls): Seq[Binding] = {
    val m                                                   = c.gameMarkerControls
    def change(f: GameMarkerControls => GameMarkerControls) = c.copy(gameMarkerControls = f(m))
    Seq(
      Binding(
        i18n.markerOnTarget(i18n.markerCross),
        m.crossTargetKey,
        code => change(_.copy(crossTargetKey = code))
      ),
      Binding(
        i18n.markerOnGround(i18n.markerCross),
        m.crossFixedKey,
        code => change(_.copy(crossFixedKey = code))
      ),
      Binding(
        i18n.markerOnTarget(i18n.markerLozenge),
        m.lozengeTargetKey,
        code => change(_.copy(lozengeTargetKey = code))
      ),
      Binding(
        i18n.markerOnGround(i18n.markerLozenge),
        m.lozengeFixedKey,
        code => change(_.copy(lozengeFixedKey = code))
      ),
      Binding(
        i18n.markerOnTarget(i18n.markerMoon),
        m.moonTargetKey,
        code => change(_.copy(moonTargetKey = code))
      ),
      Binding(
        i18n.markerOnGround(i18n.markerMoon),
        m.moonFixedKey,
        code => change(_.copy(moonFixedKey = code))
      ),
      Binding(
        i18n.markerOnTarget(i18n.markerSquare),
        m.squareTargetKey,
        code => change(_.copy(squareTargetKey = code))
      ),
      Binding(
        i18n.markerOnGround(i18n.markerSquare),
        m.squareFixedKey,
        code => change(_.copy(squareFixedKey = code))
      ),
      Binding(
        i18n.markerOnTarget(i18n.markerStar),
        m.starTargetKey,
        code => change(_.copy(starTargetKey = code))
      ),
      Binding(
        i18n.markerOnGround(i18n.markerStar),
        m.starFixedKey,
        code => change(_.copy(starFixedKey = code))
      ),
      Binding(
        i18n.markerOnTarget(i18n.markerTriangle),
        m.triangleTargetKey,
        code => change(_.copy(triangleTargetKey = code))
      ),
      Binding(
        i18n.markerOnGround(i18n.markerTriangle),
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
  box.createFontString(i18n.settings, Theme.large).setPoint(Point.TopLeft, 24, 18)

  private val tabs = Segmented[Tab](box, _.label(i18n))
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
      i18n.bindingsHelp,
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
  private val mouseOver = CheckButton(bindingsView, i18n.mouseOver)
  mouseOver.setPoint(Point.TopLeft, 470, toggleY)
  mouseOver.setScript(Script.OnClick)(_ =>
    set(controls.copy(mouseOverEnabled = mouseOver.isChecked))
  )
  private val mouseOverHelp = bindingsView.createFontString(
    i18n.mouseOverHelp,
    Theme.small,
    Theme.muted
  )
  mouseOverHelp.setWordWrap(true)
  mouseOverHelp.setWidth(390)
  mouseOverHelp.setPoint(Point.TopLeft, 470, toggleY + 28)

  private val boundTwice = bindingsView.createFontString(colour = Theme.negative.lighter(0.3))
  boundTwice.setPoint(Point.TopLeft, 24, 552)
  private val resetButton = Button(bindingsView, i18n.resetToDefaults)
  resetButton.setSize(190, 40)
  resetButton.setPoint(Point.TopLeft, 520, 540)
  resetButton.setScript(Script.OnClick)(_ => set(Pointed[Controls].unit))

  // Waiting for the key or mouse button to bind (see `capture`)
  private val captureView = view()
  private val captureText = captureView.createFontString(size = Theme.large)
  captureText.setPoint(Point.Center, box, Point.Top, 0, 265)
  captureView
    .createFontString(i18n.escapeToCancel, colour = Theme.muted)
    .setPoint(Point.Center, box, Point.Top, 0, 302)

  // The client's own settings (BFF's controls cannot hold them)
  private val generalView = view()
  private def help(text: String, y: Double): Unit = {
    val paragraph = generalView.createFontString(text, Theme.small, Theme.muted)
    paragraph.setWordWrap(true)
    paragraph.setWidth(560)
    paragraph.setPoint(Point.TopLeft, 24, y)
  }
  generalView.createFontString(i18n.language).setPoint(Point.TopLeft, 24, 88)
  private val language = Dropdown[Language](generalView, _.nativeName)
  language.setSize(200, 30)
  language.setPoint(Point.TopLeft, 214, 82)
  language.setScript(language.OnValueChanged) { chosen =>
    if !settings.language.contains(chosen) then {
      change(settings.copy(language = Some(chosen)))
      languageChanged()
    }
  }
  help(i18n.languageHelp, 118)

  generalView.createFontString(i18n.hudScale).setPoint(Point.TopLeft, 24, 148)
  private val hudScale = Selector[Double](generalView, scale => f"$scale%.1f")
  hudScale.setSize(200, 26)
  hudScale.setPoint(Point.TopLeft, 214, 144)
  hudScale.setScript(hudScale.OnValueChanged)(scale => change(settings.copy(hudScale = scale)))
  help(i18n.hudScaleHelp, 178)
  private val hostGameServers = CheckButton(generalView, i18n.hostGameServers)
  hostGameServers.setPoint(Point.TopLeft, 24, 244)
  hostGameServers.setScript(Script.OnClick)(_ =>
    change(settings.copy(hostGameServers = hostGameServers.isChecked))
  )
  help(i18n.hostGameServersHelp, 276)

  // One per volume of [[Volumes]], with how to change it
  private val volumes = Seq[(String, Volumes => Int, (Volumes, Int) => Volumes)](
    (i18n.mainVolume, _.main, (volumes, level) => volumes.copy(main = level)),
    (i18n.abilitiesVolume, _.abilities, (volumes, level) => volumes.copy(abilities = level))
  ).zipWithIndex.map { case ((label, level, changed), i) =>
    val y = 380 + 34 * i
    generalView.createFontString(label).setPoint(Point.TopLeft, 24, y + 4)
    val slider = Slider(
      generalView,
      Volumes.levels.min,
      Volumes.levels.max,
      step = 1,
      level => if level == 0 then i18n.volumeOff else level.toInt.toString
    )
    slider.setSize(260, 26)
    slider.setPoint(Point.TopLeft, 214, y)
    slider.setScript(slider.OnValueChanged)(chosen =>
      change(settings.copy(volumes = changed(settings.volumes, chosen.toInt)))
    )
    slider -> level
  }
  help(i18n.volumesHelp, 380 + 34 * volumes.size)

  // Footer
  private val footer = view()
  private val errorText =
    footer.createFontString(size = Theme.small, colour = Theme.negative.lighter(0.3))
  errorText.setPoint(Point.TopLeft, 24, 526)
  private val doneButton = Button(footer, i18n.done, ButtonStyle.Primary)
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

    for (column, i) <- waiting do captureText.setText(i18n.pressKeyFor(column(controls)(i).action))
    for (button, label) <- bindingButtons do button.setText(label(controls))
    mouseOver.setChecked(controls.mouseOverEnabled)
    boundTwice.setText(
      controls.maybeMultipleKey.fold("")(key => i18n.boundToSeveralActions(key.label))
    )

    language.setOptions(Language.values.toSeq, settings.language)
    hudScale.setOptions(ClientSettings.hudScales, Some(settings.hudScale))
    hostGameServers.setChecked(settings.hostGameServers)
    for (slider, level) <- volumes do slider.setValue(level(settings.volumes))

    errorText.setText(error.getOrElse(""))
  }
}
