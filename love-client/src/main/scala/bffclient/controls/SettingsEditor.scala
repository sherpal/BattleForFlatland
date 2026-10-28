package bffclient.controls

import bffclient.love.LoveInput
import bffclient.ui.*
import models.bff.ingame.{Controls, GameMarkerControls}
import models.bff.ingame.Controls.InputCode
import models.syntax.Pointed

/** Changes the settings, in two tabs:
  *   - the controls, like the frontend's `ControlSettings`: click a binding, then press the key
  *     (possibly with Shift) or the mouse button to bind to it. Every change is saved to
  *     `controls.json` (see [[ControlsFile]]), which the next game loads;
  *   - the client's own settings ([[ClientSettings]]), saved to `settings.json`.
  *
  * Drawn over the menus (which stay open behind, e.g. the lobby and its websocket).
  */
final class SettingsEditor(close: () => Unit) {
  private var controls = ControlsFile.load()._1
  private var settings = ClientSettings.current
  private var error    = Option.empty[String]

  private enum Tab(val label: String) {
    case Bindings extends Tab("Controls")
    case General  extends Tab("General")
  }
  private var tab = Tab.Bindings

  /** The action whose binding is being changed, and how to change it. */
  private var waiting = Option.empty[(String, InputCode => Controls)]

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
  }

  private def change(newSettings: ClientSettings): Unit = {
    settings = newSettings
    error = settings.save()
  }

  def draw(ui: Ui, width: Double, height: Double): Unit = {
    ui.fill(Rect(0, 0, width, height), Theme.background.withAlpha(0.75))
    val box = Rect(width / 2 - 450, height / 2 - 300, 900, 600)
    ui.panel(box)
    ui.text("Settings", box.x + 24, box.y + 18, Theme.large)
    ui.segmented(
      Tab.values.toSeq,
      Some(tab),
      Rect(box.right - 284, box.y + 16, 260, 32),
      (_: Tab).label,
      waiting.isEmpty
    ).foreach(tab = _)

    (tab, waiting) match {
      case (Tab.Bindings, Some((action, change))) => capture(ui, box, action, change)
      case (Tab.Bindings, None) => bindings(ui, box); footer(ui, box)
      case (Tab.General, _) => general(ui, box); footer(ui, box)
    }
  }

  /** Waits for the key or mouse button to bind. */
  private def capture(ui: Ui, box: Rect, action: String, change: InputCode => Controls): Unit = {
    ui.textIn(
      s"Press a key or a mouse button for \"$action\"",
      Rect(box.x, box.y + 250, box.width, 30),
      Theme.large
    )
    ui.textIn("Escape to cancel", Rect(box.x, box.y + 290, box.width, 24), colour = Theme.muted)
    val modifiers = Set("lshift", "rshift")
    if ui.wasPressed("escape") then waiting = None
    else
      ui.pressedKeys
        .filterNot(modifiers)
        .flatMap(LoveInput.inputCode)
        .headOption
        .orElse(ui.pressedButtons.headOption.map(LoveInput.mouseCode)) match {
        case Some(code) => set(change(code)); waiting = None
        case None => ()
      }
  }

  private def bindings(ui: Ui, box: Rect): Unit = {
    ui.text(
      "Click a binding, then press a key (possibly with Shift) or a mouse button. Changes apply to the next game.",
      box.x + 24,
      box.y + 52,
      Theme.small,
      Theme.muted
    )
    def column(bindings: Seq[Binding], left: Double): Unit =
      for (binding, i) <- bindings.zipWithIndex do {
        val y = box.y + 84 + 25 * i
        ui.text(binding.action, left, y + 3)
        if ui.button(binding.code.label, Rect(left + 190, y, 200, 23)) then
          waiting = Some((binding.action, binding.change))
      }
    column(movementsAndAbilities(controls), box.x + 24)
    val markerBindings = markers(controls)
    column(markerBindings, box.x + 470)

    val toggleY = box.y + 84 + 25 * markerBindings.size + 12
    if ui.checkbox(controls.mouseOverEnabled, "Mouse over", box.x + 470, toggleY) then
      set(controls.copy(mouseOverEnabled = !controls.mouseOverEnabled))
    ui.paragraph(
      "Abilities fall back to the frame under the mouse when the target is not valid for them.",
      box.x + 470,
      toggleY + 28,
      390,
      Theme.small,
      Theme.muted
    )

    val bottom = box.bottom - 60
    for key <- controls.maybeMultipleKey do
      ui.text(
        s"${key.label} is bound to several actions",
        box.x + 24,
        bottom + 12,
        colour = Theme.negative.lighter(0.3)
      )
    if ui.button("Reset to defaults", Rect(box.right - 380, bottom, 190, 40)) then
      set(Pointed[Controls].unit)
  }

  /** The client's own settings (BFF's controls cannot hold them). */
  private def general(ui: Ui, box: Rect): Unit = {
    val left  = box.x + 24
    val width = 560.0

    val scaleY = box.y + 84
    ui.text("HUD scale", left, scaleY + 4)
    ui.selector(
      ClientSettings.hudScales,
      settings.hudScale,
      Rect(left + 190, scaleY, 200, 26),
      (scale: Double) => f"$scale%.1f"
    ).foreach(scale => change(settings.copy(hudScale = scale)))
    ui.paragraph(
      "The size of the frames and of the action bar, whatever the window's size. It applies to the next game.",
      left,
      scaleY + 34,
      width,
      Theme.small,
      Theme.muted
    )

    val hostY = scaleY + 100
    if ui.checkbox(settings.hostGameServers, "Host game servers", left, hostY) then
      change(settings.copy(hostGameServers = !settings.hostGameServers))
    ui.paragraph(
      "The game server of a game runs on the computer of one of its players. To be ready to run it, " +
        "the client downloads it, with what runs it (several tens of MB, once), as soon as you join " +
        "a game. Turn this off to download nothing, e.g. on a mobile network: the other players " +
        "will run it.",
      left,
      hostY + 32,
      width,
      Theme.small,
      Theme.muted
    )
  }

  private def footer(ui: Ui, box: Rect): Unit = {
    val bottom = box.bottom - 60
    for message <- error do
      ui.text(message, box.x + 24, bottom - 14, Theme.small, Theme.negative.lighter(0.3))
    if
      ui.button("Done", Rect(box.right - 174, bottom, 150, 40), style = ButtonStyle.Primary) || ui
        .wasPressed("escape")
    then
      close()
  }
}
