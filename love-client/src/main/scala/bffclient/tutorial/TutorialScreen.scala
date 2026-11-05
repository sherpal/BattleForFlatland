package bffclient.tutorial

import scala.concurrent.ExecutionContext

import bffclient.gui.*
import bffclient.menu.{MenuApp, Screen}
import bffclient.ui.{Colour, Theme}
import menus.data.User
import services.i18n.I18n

/** The tutorial's entry: a panel per [[Topic]] (how the game works, then each class), each one
  * opening its page.
  */
final class TutorialScreen(app: MenuApp, user: User)(using ExecutionContext) extends Screen {
  private val i18n = app.i18n

  if !app.preferences.tutorialVisited then app.updatePreferences(_.copy(tutorialVisited = true))

  private val root = Frame(app.gui.root)
  root.setAllPoints()

  root.createFontString(i18n.learnToPlay, Theme.title).setPoint(Point.TopLeft, 40, 30)
  private val intro = root.createFontString(i18n.tutorialIntro, colour = Theme.muted)
  intro.setWordWrap(true)
  intro.setPoint(Point.TopLeft, 40, 80)
  intro.setPoint(Point.TopRight, -40, 80)

  // under the intro, which may take two lines
  private val row = Frame(root)
  row.setPoint(Point.TopLeft, intro, Point.BottomLeft, 0, 28)
  row.setPoint(Point.TopRight, intro, Point.BottomRight, 0, 28)
  row.setHeight(340)

  private val count = Topic.values.length
  for topic <- Topic.values do {
    val card = TopicCard(row, topic, i18n, () => app.show(TopicScreen(app, user, topic)))
    val i    = topic.ordinal
    card.frame.setPoint(
      Point.TopLeft,
      row,
      Point(i.toDouble / count, 0),
      if i == 0 then 0 else 8,
      0
    )
    card.frame.setPoint(
      Point.BottomRight,
      row,
      Point((i + 1.0) / count, 1),
      if i == count - 1 then 0 else -8,
      0
    )
  }

  private val hint = root.createFontString(i18n.tutorialHint, colour = Theme.muted)
  hint.setPoint(Point.TopLeft, row, Point.BottomLeft, 0, 24)

  private val back = Button(root, i18n.backToTheGames)
  back.setSize(200, 44)
  back.setPoint(Point.BottomLeft, 40, -26)
  back.setScript(Script.OnClick)(_ => app.show(app.home(user)))

  def leave(): Unit = root.remove()
}

/** A panel of the tutorial's entry: its picture, title, role and summary, lit when hovered, which
  * calls `open` when clicked.
  */
private final class TopicCard(parent: Frame, topic: Topic, i18n: I18n, open: () => Unit) {
  val frame: Frame = Frame(parent)
  frame.setBackdrop(Backdrop(Theme.panel, Some(Theme.border), 8))
  frame.enableMouse(true)
  frame.setScript(Script.OnClick)(_ => open())

  private val stripe = frame.createTexture(topic.colour, Layer.Border)
  stripe.setPoint(Point.TopLeft, 1, 1)
  stripe.setPoint(Point.TopRight, -1, 1)
  stripe.setHeight(5)
  stripe.setCornerRadius(2)

  private val lit = frame.createTexture(topic.colour.withAlpha(0.08), Layer.Highlight)
  lit.setAllPoints(frame)
  lit.setCornerRadius(8)
  lit.setOutline(Some(topic.colour), 2)
  lit.hide()
  frame.setScript(Script.OnEnter)(() => lit.show())
  frame.setScript(Script.OnLeave)(() => lit.hide())

  // The picture: the class' shape, or the whole team around the boss, centred in a row
  private val pictureRow = Frame(frame)
  pictureRow.setPoint(Point.TopLeft, 0, 22)
  pictureRow.setPoint(Point.TopRight, 0, 22)
  pictureRow.setHeight(100)
  private val picture = Frame(pictureRow)
  picture.setSize(120, 100)
  picture.setPoint(Point.Center)
  topic.playerClass match {
    case Some(cls) => Shapes.player(picture, cls, topic.colour, 60, 50, 64)
    case None => Shapes.team(picture)
  }

  /** A centred text under `above`, as wide (less `inset` on each side). */
  private def line(
      text: String,
      size: Int,
      colour: Colour,
      above: Region,
      gap: Double,
      inset: Double = 0
  ): FontString = {
    val fontString = frame.createFontString(text, size, colour)
    fontString.setWordWrap(true)
    fontString.setJustifyH(JustifyH.Center)
    fontString.setPoint(Point.TopLeft, above, Point.BottomLeft, inset, gap)
    fontString.setPoint(Point.TopRight, above, Point.BottomRight, -inset, gap)
    fontString
  }
  private val title = line(topic.title(i18n), Theme.large, Theme.text, pictureRow, 14, inset = 12)
  private val role  = line(topic.role(i18n), Theme.body, topic.colour, title, 4)
  line(topic.summary(i18n), Theme.small, Theme.muted, role, 14)
}
