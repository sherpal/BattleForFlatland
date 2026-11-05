package bffclient.tutorial

import scala.concurrent.ExecutionContext

import bffclient.controls.ControlsFile
import bffclient.gui.*
import bffclient.menu.{GameLobbyScreen, MenuApp, MenuColours, Screen}
import bffclient.ui.{ButtonStyle, Colour, Theme}
import menus.data.User
import models.bff.outofgame.PlayerClasses

/** A page of the tutorial: what it says on the left (scrolled with the mouse wheel), and on the
  * right the HUD as it is in a fight (its own frames, with a made-up game state), with marks that
  * its legend explains. For the overview, also where everyone stands in the arena.
  *
  * The tabs on top go to the other pages. A class' page sets up a practice fight against Boss 101,
  * with AIs, after which the menus come back here.
  */
final class TopicScreen(app: MenuApp, user: User, topic: Topic)(using ExecutionContext)
    extends Screen {
  private val controls = ControlsFile.load()._1
  private var busy     = false

  private val root = Frame(app.gui.root)
  root.setAllPoints()

  // Header: the shape, title and role, and the tabs of the pages
  private val icon = Frame(root)
  icon.setSize(60, 56)
  icon.setPoint(Point.TopLeft, 36, 26)
  topic.playerClass match {
    case Some(cls) => Shapes.player(icon, cls, topic.colour, 30, 28, 50)
    case None => Shapes.team(icon, 0.5)
  }
  private val title = root.createFontString(topic.title, Theme.title)
  title.setPoint(Point.TopLeft, 106, 22)
  root
    .createFontString(topic.role, Theme.body, topic.colour)
    .setPoint(Point.TopLeft, title, Point.BottomLeft, 0, 2)

  private val tabs = Segmented[Topic](root, _.title.stripPrefix("The "))
  tabs.setSize(520, 36)
  tabs.setPoint(Point.TopRight, -40, 34)
  tabs.setOptions(Topic.values.toSeq, Some(topic))
  tabs.setScript(tabs.OnValueChanged)(other => app.show(TopicScreen(app, user, other)))

  // The texts, on the left
  private val split = 0.38
  private val texts = Templates.panel(root)
  texts.setPoint(Point.TopLeft, 40, 104)
  texts.setPoint(Point.BottomRight, root, Point(split, 1), 0, -90)
  private val scroll = ScrollFrame(texts)
  scroll.setPoint(Point.TopLeft, 1, 8)
  scroll.setPoint(Point.BottomRight, -1, -8)
  scroll.setScrollStep(40)
  scroll.setFitsContents(true)
  private val contents = Frame(scroll.scrollChild)
  contents.setPoint(Point.TopLeft)
  contents.setPoint(Point.TopRight)
  writeTexts()

  // The preview, on the right: tabs (for the overview), the canvas, its legend
  private val preview = Templates.panel(root)
  preview.setPoint(Point.TopLeft, root, Point(split, 0), 20, 104)
  preview.setPoint(Point.BottomRight, -40, -90)

  private val scene = TutorialScene(
    topic.playerClass.getOrElse(PlayerClasses.Triangle),
    user.name,
    app.preferences.colour.getOrElse(MenuColours.palette.head),
    controls
  )
  private val legendLines = 5
  private val legend      = Frame(preview)
  legend.setPoint(Point.BottomLeft, 16, -10)
  legend.setPoint(Point.BottomRight, -16, -10)
  legend.setHeight(legendLines * 22.0)

  private val previewTabs = Segmented[String](preview)
  previewTabs.setSize(240, 32)
  previewTabs.setPoint(Point.TopLeft, 12, 10)
  previewTabs.setShown(topic == Topic.Overview)

  private val area = Frame(preview)
  area.setPoint(Point.TopLeft, 12, if topic == Topic.Overview then 52 else 12)
  area.setPoint(Point.BottomRight, legend, Point.TopRight, 4, -10)

  private val hud   = HudPreview(preview, area, topic, scene)
  private val arena = Option.when(topic == Topic.Overview)(ArenaPreview(preview, area))
  private val hint  = preview.createFontString(colour = Theme.muted, size = Theme.small)
  hint.setPoint(Point.TopRight, -14, 18)

  /** The legend of the marks (or a word on the arena). */
  private val legendRows = (0 until legendLines).map { i =>
    val mark = Mark(legend, i + 1, topic.colour)
    mark.setPoint(Point.TopLeft, 0, i * 22.0)
    val text = legend.createFontString(size = Theme.small)
    text.setPoint(Point.Left, mark, Point.Right, 8, 0)
    (mark, text)
  }

  private val arenaTab = "Arena"
  private val hudTab   = "Screen"
  private def showPreview(tab: String): Unit = {
    val showArena = arena.isDefined && tab == arenaTab
    previewTabs.setOptions(if arena.isDefined then Seq(arenaTab, hudTab) else Nil, Some(tab))
    arena.foreach(_.frame.setShown(showArena))
    hud.frame.setShown(!showArena)
    hint.setText(if showArena then "" else "Hover the abilities, click the frames")
    // the arena's legend has no marks
    val lines = if showArena then arena.fold(Nil)(_.legend) else hud.legend
    for ((mark, text), i) <- legendRows.zipWithIndex do {
      mark.setShown(!showArena && i < lines.size)
      text.setText(lines.lift(i).getOrElse(""))
    }
  }
  previewTabs.setScript(previewTabs.OnValueChanged)(showPreview)
  showPreview(if arena.isDefined then arenaTab else hudTab)

  // Actions
  private val back = Button(root, "Back to the tutorial")
  back.setSize(210, 44)
  back.setPoint(Point.BottomLeft, 40, -26)
  back.setScript(Script.OnClick)(_ => app.show(TutorialScreen(app, user)))

  private val practice = Button(root, "Practice against Boss 101", ButtonStyle.Positive)
  practice.setSize(270, 44)
  practice.setPoint(Point.BottomRight, -40, -26)
  practice.setShown(topic.playerClass.isDefined)
  practice.setScript(Script.OnClick)(_ => topic.playerClass.foreach(startPractice))

  for next <- topic.next do {
    // after the overview, the way to go
    val style  = if topic.playerClass.isEmpty then ButtonStyle.Primary else ButtonStyle.Normal
    val button = Button(root, s"Next: ${next.title}", style)
    button.setSize(220, 44)
    if topic.playerClass.isDefined then
      button.setPoint(Point.BottomRight, practice, Point.BottomLeft, -16, 0)
    else button.setPoint(Point.BottomRight, -40, -26)
    button.setScript(Script.OnClick)(_ => app.show(TopicScreen(app, user, next)))
  }

  def leave(): Unit = root.remove()

  /** The sections, one under the other: headings in the topic's colour, paragraphs, and items (the
    * lines starting with "- ") after a dot.
    */
  private def writeTexts(): Unit = {
    val margin    = 18.0
    val itemInset = 16.0
    var previous  = Option.empty[(FontString, Double)] // and its inset
    def add(text: String, size: Int, colour: Colour, gap: Double, inset: Double): FontString = {
      val fontString = contents.createFontString(text, size, colour)
      fontString.setWordWrap(true)
      previous match {
        case Some((above, aboveInset)) =>
          fontString.setPoint(Point.TopLeft, above, Point.BottomLeft, inset - aboveInset, gap)
          fontString.setPoint(Point.TopRight, above, Point.BottomRight, 0, gap)
        case None =>
          fontString.setPoint(Point.TopLeft, contents, Point.TopLeft, margin + inset, gap)
          fontString.setPoint(Point.TopRight, contents, Point.TopRight, -margin, gap)
      }
      previous = Some((fontString, inset))
      fontString
    }

    for (section, i) <- TutorialTexts.sections(topic, controls).zipWithIndex do {
      add(section.heading, Theme.large, topic.colour, if i == 0 then 4 else 22, 0)
      for paragraph <- section.paragraphs do
        if paragraph.startsWith("- ") then {
          val item = add(paragraph.drop(2), TopicScreen.textSize, Theme.text, 6, itemInset)
          val dot  = contents.createTexture(topic.colour, Layer.Artwork)
          dot.setSize(6, 6)
          dot.setCornerRadius(3)
          dot.setPoint(Point.Center, item, Point.TopLeft, -itemInset / 2, 9)
        } else add(paragraph, TopicScreen.textSize, Theme.text, 8, 0)
    }
  }

  /** Sets up the game, then goes to its lobby, which launches it; the menus come back here after
    * it.
    */
  private def startPractice(cls: PlayerClasses): Unit = if !busy then {
    busy = true
    practice.setEnabled(false)
    practice.setText("Setting up the game...")
    val colour = app.preferences.colour.getOrElse(MenuColours.palette.head)
    app.attempt(Practice.create(app.api, user))(
      gameId => {
        app.returnAfterGame(TopicScreen(app, _, topic))
        def enter(): Unit = app.show(GameLobbyScreen(app, user, gameId, launchWhenReady = true))
        // if it cannot be set up, the lobby shows what is missing
        app.attempt(Practice.setUp(app.api, user, gameId, cls, colour))(_ => enter(), _ => enter())
      },
      _ => {
        busy = false
        practice.setEnabled(true)
        practice.setText("Practice against Boss 101")
      }
    )
  }
}

object TopicScreen {
  private val textSize = 15
}
