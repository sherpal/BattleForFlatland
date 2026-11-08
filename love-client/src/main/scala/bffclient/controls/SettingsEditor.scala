package bffclient.controls

import bffclient.gui.*
import bffclient.hud.HudStyle.softened
import bffclient.hud.HudWidgets.setSprite
import bffclient.love.{LoveInput, Mouse}
import bffclient.render.Sprites
import bffclient.ui.{ButtonStyle, Colour, Theme}
import gamelogic.abilities.Ability
import models.bff.ingame.{Controls, GameMarkerControls}
import models.bff.ingame.Controls.InputCode
import models.bff.outofgame.PlayerClasses
import models.syntax.Pointed
import services.i18n.{I18n, Language}

/** Changes the settings, in three tabs:
  *   - the controls, like the frontend's `ControlSettings`: click a binding, then press the key
  *     (possibly with Shift) or the mouse button to bind to it. Every change is saved to
  *     `controls.json` (see [[ControlsFile]]), which the next game loads;
  *   - the order of the abilities in the action bar, per class: drag an ability (from the list of
  *     the class's abilities, or from the bar) onto a slot of the bar, whose ability takes its
  *     place. Saved with the client's settings;
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
    case Abilities extends Tab(_.actionBar)
    case General extends Tab(_.general)
  }
  private var tab = if onGeneral then Tab.General else Tab.Bindings

  /** The action whose binding is being changed: its column and its index there. */
  private var waiting = Option.empty[(Controls => Seq[Binding], Int)]

  /** The class whose action bar is shown, and the ability being dragged onto it. */
  private var playerClass = PlayerClasses.allChoices.head
  private var dragged     = Option.empty[Ability.AbilityId]

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
  tabs.setSize(390, 32)
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
      button -> ((c: Controls) => LoveInput.label(column(c)(i).code))
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

  // The order of the abilities in the action bar
  private val abilitiesView = view()
  private val actionBarHelp =
    abilitiesView.createFontString(i18n.actionBarHelp, Theme.small, Theme.muted)
  actionBarHelp.setWordWrap(true)
  actionBarHelp.setWidth(852)
  actionBarHelp.setPoint(Point.TopLeft, 24, 52)

  private val classes = Segmented[PlayerClasses](abilitiesView, i18n.playerClassName)
  classes.setSize(480, 32)
  classes.setPoint(Point.TopLeft, 24, 92)
  classes.setScript(classes.OnValueChanged) { chosen =>
    playerClass = chosen
    render()
  }

  /** An ability's icon (or its initials when it has none) on a texture of `frame`. */
  private final class AbilityIcon(frame: Frame, size: Double) {
    val texture: Texture = frame.createTexture(Layer.Artwork)
    texture.setSize(size, size)
    private val initials = frame.createFontString(size = Theme.small, layer = Layer.Overlay)
    initials.setAllPoints(texture)
    initials.setJustifyH(JustifyH.Center)

    def show(ability: Ability.AbilityId): Unit = Sprites.abilityIcons.get(ability) match {
      case Some(sprite) =>
        setSprite(texture, sprite)
        texture.setColour(Colour(1, 1, 1))
        texture.setCornerRadius(0)
        initials.setText("")
      case None =>
        texture.clearImage()
        texture.setColour(Theme.background)
        texture.setCornerRadius(4)
        initials.setText(abilityName(ability).split(' ').map(_.take(1)).mkString)
    }
  }

  private def abilityName(ability: Ability.AbilityId): String =
    playerClass.builder.abilitiesDoc.find(_.abilityId == ability).fold(s"#$ability")(_.name)

  /** A frame from which the ability it shows is dragged (from where the mouse is pressed). */
  private def draggable(frame: Frame, ability: () => Option[Ability.AbilityId]): Unit = {
    frame.enableMouse(true)
    frame.registerForClicks(1)
    frame.setScript(Script.OnMouseDown) { _ =>
      dragged = ability()
      render()
    }
    frame.setScript(Script.OnMouseUp)(_ => drop())
  }

  private val maxAbilities = PlayerClasses.allChoices.map(_.builder.abilities.size).max

  /** The class's abilities (always in the same order), with their slot in the bar. */
  private final class Card(index: Int) {
    var ability = Option.empty[Ability.AbilityId]

    val frame: Frame = Frame(abilitiesView)
    frame.setSize(276, 60)
    frame.setPoint(Point.TopLeft, 24 + 288 * (index % 3), 148 + 70 * (index / 3))
    draggable(frame, () => ability)
    private val icon = AbilityIcon(frame, 44)
    icon.texture.setPoint(Point.Left, frame, Point.Left, 8, 0)
    private val name = frame.createFontString()
    name.setPoint(Point.TopLeft, frame, Point.TopLeft, 62, 10)
    private val slot = frame.createFontString(size = Theme.small, colour = Theme.muted)
    slot.setPoint(Point.TopLeft, frame, Point.TopLeft, 62, 34)

    def show(shown: Option[Ability.AbilityId], order: Vector[Ability.AbilityId]): Unit = {
      ability = shown
      frame.setShown(shown.isDefined)
      for id <- shown do {
        icon.show(id)
        name.setText(abilityName(id))
        val index = order.indexOf(id)
        slot.setText(s"${i18n.abilityNumber(index + 1)}  ·  ${keyLabel(index)}")
      }
    }
  }
  private val cards = Vector.tabulate(maxAbilities)(Card(_))

  /** The slots of the action bar, centred, with their key under them. */
  private final class Slot(index: Int) {
    var ability = Option.empty[Ability.AbilityId]

    val frame: Frame = Frame(abilitiesView)
    frame.setSize(Slot.size, Slot.size)
    draggable(frame, () => ability)
    private val icon = AbilityIcon(frame, 44)
    icon.texture.setPoint(Point.Center, frame, Point.Center, 0, -2)
    // The colour of the ability, as a line at the bottom (like in the game's action bar)
    private val colourLine = frame.createTexture(Layer.Artwork)
    colourLine.setPoint(Point.BottomLeft, frame, Point.BottomLeft, 8, -4)
    colourLine.setSize(Slot.size - 16, 2)
    colourLine.setCornerRadius(1)
    private val key = abilitiesView.createFontString(size = Theme.small, colour = Theme.muted)
    key.setPoint(Point.Top, frame, Point.Bottom, 0, 6)

    def show(shown: Option[Ability.AbilityId], slots: Int): Unit = {
      ability = shown
      frame.setShown(shown.isDefined)
      key.setShown(shown.isDefined)
      frame.setPoint(
        Point.TopLeft,
        (900 - slots * Slot.size - (slots - 1) * Slot.gap) / 2 + index * (Slot.size + Slot.gap),
        412
      )
      for id <- shown do {
        icon.show(id)
        colourLine.setColour(softened(Ability.abilityColour(id)))
        key.setText(keyLabel(index))
      }
    }
  }
  private object Slot {
    val size = 60.0
    val gap  = 10.0
  }
  abilitiesView.createFontString(i18n.actionBar).setPoint(Point.TopLeft, 24, 376)
  private val slots = Vector.tabulate(maxAbilities)(Slot(_))

  private def keyLabel(index: Int): String =
    controls.abilityKeys.lift(index).fold("-")(LoveInput.label)

  // What is dragged, under the mouse
  private val ghost = Frame(frame)
  ghost.setFrameStrata(Strata.Tooltip)
  ghost.setSize(52, 52)
  ghost.setBackdrop(Backdrop(Theme.field, Some(Theme.primary.lighter(0.3)), 6))
  ghost.setAlpha(0.85)
  private val ghostIcon = AbilityIcon(ghost, 44)
  ghostIcon.texture.setPoint(Point.Center, ghost, Point.Center)

  /** The mouse, in the frames' coordinates. */
  private def mouse: (Double, Double) = {
    val position = Mouse.getPosition()
    val scale    = frame.effectiveScale
    (position.x / scale, position.y / scale)
  }

  /** The index of the slot under the mouse. */
  private def slotUnderMouse: Option[Int] = {
    val (x, y) = mouse
    Some(
      slots.indexWhere(slot =>
        slot.ability.isDefined && x >= slot.frame.left && x < slot.frame.right &&
          y >= slot.frame.top && y < slot.frame.bottom
      )
    ).filter(_ >= 0)
  }

  abilitiesView.setScript(Script.OnUpdate) { _ =>
    val target = dragged.flatMap(_ => slotUnderMouse)
    val (x, y) = mouse
    if dragged.isDefined then ghost.setPoint(Point.Center, frame, Point.TopLeft, x, y)
    for (slot, i) <- slots.zipWithIndex do
      slot.frame.setBackdrop(
        Backdrop(
          Theme.field,
          Some(
            if target.contains(i) then Theme.primary.lighter(0.3)
            else if dragged.isEmpty && frame.gui.isHovered(slot.frame) then Theme.muted
            else Theme.border
          ),
          8
        )
      )
    for card <- cards do
      card.frame.setBackdrop(
        Backdrop(
          Theme.panel.lighter(0.05),
          Some(
            if dragged.isEmpty && frame.gui.isHovered(card.frame) then Theme.muted
            else Theme.border
          ),
          6
        )
      )
  }

  /** The dragged ability goes to the slot under the mouse, and that slot's to where it was. */
  private def drop(): Unit = {
    val ability = dragged
    dragged = None
    (for
      id   <- ability
      slot <- slotUnderMouse
    yield {
      val order = settings.abilityOrder(playerClass)
      order.updated(order.indexOf(id), order(slot)).updated(slot, id)
    }) match {
      case Some(order) => change(settings.withAbilityOrder(playerClass, order))
      case None => render()
    }
  }

  private val resetOrder = Button(abilitiesView, i18n.resetToDefaults)
  resetOrder.setSize(190, 40)
  resetOrder.setPoint(Point.TopLeft, 520, 540)
  resetOrder.setScript(Script.OnClick)(_ =>
    change(settings.withAbilityOrder(playerClass, playerClass.builder.abilities.toVector))
  )

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
      case None if dragged.isDefined =>
        if scancode == "escape" then { dragged = None; render() }
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
    abilitiesView.setShown(tab == Tab.Abilities)
    generalView.setShown(tab == Tab.General)
    footer.setShown(!(tab == Tab.Bindings && waiting.isDefined))

    for (column, i) <- waiting do captureText.setText(i18n.pressKeyFor(column(controls)(i).action))
    for (button, label) <- bindingButtons do button.setText(label(controls))
    mouseOver.setChecked(controls.mouseOverEnabled)
    boundTwice.setText(
      controls.maybeMultipleKey.fold("")(key => i18n.boundToSeveralActions(LoveInput.label(key)))
    )

    classes.setOptions(PlayerClasses.allChoices, Some(playerClass))
    val abilities = playerClass.builder.abilities.toVector
    val order     = settings.abilityOrder(playerClass)
    for (card, i) <- cards.zipWithIndex do card.show(abilities.lift(i), order)
    for (slot, i) <- slots.zipWithIndex do slot.show(order.lift(i), order.size)
    // what is dragged is dimmed where it comes from
    def dimmed(ability: Option[Ability.AbilityId]) = dragged.isDefined && ability == dragged
    for card <- cards do card.frame.setAlpha(if dimmed(card.ability) then 0.4 else 1)
    for slot <- slots do slot.frame.setAlpha(if dimmed(slot.ability) then 0.4 else 1)
    ghost.setShown(tab == Tab.Abilities && dragged.isDefined)
    for id <- dragged do ghostIcon.show(id)
    resetOrder.setEnabled(settings.abilityOrders.contains(playerClass))

    language.setOptions(Language.values.toSeq, settings.language)
    hudScale.setOptions(ClientSettings.hudScales, Some(settings.hudScale))
    hostGameServers.setChecked(settings.hostGameServers)
    for (slider, level) <- volumes do slider.setValue(level(settings.volumes))

    errorText.setText(error.getOrElse(""))
  }
}
