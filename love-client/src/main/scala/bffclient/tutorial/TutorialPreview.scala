package bffclient.tutorial

import bffclient.gui.*
import bffclient.hud.*
import bffclient.hud.HudWidgets.*
import bffclient.love.Graphics
import bffclient.render.Sprites
import bffclient.ui.{Colour, Theme}
import gamelogic.entities.Entity
import gamelogic.entities.classes.Constants
import models.bff.outofgame.PlayerClasses
import services.i18n.I18n

/** A frame of a fixed size (in its units), centred in `area`, and scaled down to fit in it, on the
  * arena's grey with the aztec diamond (half transparent, as in the game).
  */
private class Canvas(parent: Frame, area: Region, val width: Double, val height: Double) {
  val frame: Frame = Frame(parent)
  frame.setSize(width, height)
  frame.setPoint(Point.Center, area, Point.Center)
  frame.setClipsChildren(true)
  frame.setScript(Script.OnUpdate) { _ =>
    val scale = math.min(1.0, math.min(area.width / width, area.height / height))
    if math.abs(scale - frame.getScale) > 0.001 then frame.setScale(scale)
  }

  frame.setBackdrop(Backdrop(Colour.rgb(200, 200, 200), Some(Colour(0, 0, 0, 0.5)), 4))
  for background <- Sprites.aztecDiamond do {
    val texture = frame.createTexture(Colour(1, 1, 1, 0.5), Layer.Background)
    // a part of its centre, drawn twice as big
    val (partWidth, partHeight)   = (width / 2, height / 2)
    val (imageWidth, imageHeight) = (background.image.getWidth(), background.image.getHeight())
    val quad = Graphics.newQuad(
      (imageWidth - partWidth) / 2,
      (imageHeight - partHeight) / 2,
      partWidth,
      partHeight,
      imageWidth,
      imageHeight
    )
    texture.setImage(background.image, quad, partWidth, partHeight)
    texture.setAllPoints(frame)
  }
}

/** Shapes of the arena, drawn with textures, centred at (x, y) of their frame's top left corner:
  * the boss as a white disc, the players as their class' sprite, tinted with their colour.
  */
private object Shapes {

  /** The boss, with the team around it (the Triangle behind, the Square in front, ...), in the
    * tutorial's colours, in a frame of 120 x 100 (times `scale`).
    */
  def team(frame: Frame, scale: Double = 1.0): Unit = {
    boss(frame, 60 * scale, 50 * scale, 22 * scale)
    for (topic, (x, y)) <- Seq(
        Topic.Triangle -> (60, 12),
        Topic.Square   -> (60, 88),
        Topic.Pentagon -> (14, 64),
        Topic.Hexagon  -> (106, 64)
      )
    do topic.playerClass.foreach(player(frame, _, topic.colour, x * scale, y * scale, 26 * scale))
  }

  def boss(frame: Frame, x: Double, y: Double, radius: Double): Texture = {
    val disc = frame.createTexture(Colour(1, 1, 1), Layer.Artwork)
    disc.setSize(2 * radius, 2 * radius)
    disc.setPoint(Point.Center, frame, Point.TopLeft, x, y)
    disc.setCornerRadius(radius)
    disc.setOutline(Some(Colour(0, 0, 0)), 2)
    disc
  }

  def player(
      frame: Frame,
      cls: PlayerClasses,
      colour: Colour,
      x: Double,
      y: Double,
      size: Double
  ): Texture = {
    val shape = frame.createTexture(colour, Layer.Artwork)
    Sprites.playerClasses.get(cls.value) match {
      case Some(sprite) =>
        setSprite(shape, sprite)
        shape.setSize(size, size * sprite.frameHeight / sprite.frameWidth)
      case None =>
        shape.setSize(size * 0.8, size * 0.8)
        shape.setCornerRadius(3)
    }
    shape.setPoint(Point.Center, frame, Point.TopLeft, x, y)
    shape
  }
}

/** A numbered mark, to point at a part of a preview (and in the legend that says what it is). */
private object Mark {
  val size = 22.0

  def apply(parent: Frame, number: Int, colour: Colour): Frame = {
    val frame = Frame(parent)
    frame.setSize(size, size)
    frame.setFrameLevel(parent.frameLevel + 20) // over the HUD's frames
    val disc = frame.createTexture(colour, Layer.Artwork)
    disc.setAllPoints(frame)
    disc.setCornerRadius(size / 2)
    disc.setOutline(Some(Colour(0, 0, 0, 0.8)), 2)
    textOver(frame, frame, JustifyH.Center, size = HudStyle.titleSize, padding = 0)
      .setText(number.toString)
    frame
  }
}

/** The HUD as it is in a fight (the HUD's own frames, showing a [[TutorialScene]]), on a canvas of
  * the size of a small window, with the topic's marks on the parts that its legend explains.
  */
final class HudPreview(parent: Frame, area: Region, topic: Topic, scene: TutorialScene) {
  private val canvas = Canvas(parent, area, 780, 460)
  val frame: Frame   = canvas.frame
  private val root   = Frame(frame)
  root.setAllPoints()

  // The arena, under the HUD: the boss and the players where the scene puts them
  private val centreX = canvas.width / 2
  private val centreY = 190.0
  private val zoom    = 0.4
  Shapes.boss(root, centreX, centreY, Constants.bossRadius * zoom)
  for
    id     <- scene.teamIds
    player <- scene.gameState.players.get(id)
  do {
    val (r, g, b) = player.rgb
    Shapes.player(
      root,
      player.cls,
      Colour.rgb(r, g, b),
      centreX + player.pos.re * zoom,
      centreY - player.pos.im * zoom,
      3 * Constants.playerRadius * zoom
    )
  }

  // The HUD, placed as in the game (see Hud)
  private val noVisuals = () => None
  private val boss      = BossFrame(root, scene)
  private val cooldowns = BossCooldowns(root, scene.i18n)
  private val threat    = ThreatMeter(root, scene.i18n)
  private val damage    = DamageMeter(root, threat.frame, scene.i18n)
  private val actionBar = ActionBar(root, scene)
  private val myFrame   = UnitFrame(root, scene, noVisuals, 200, isTarget = false)
  private val target    = UnitFrame(root, scene, noVisuals, 200, isTarget = true)
  private val brace     = BraceGauge(root)
  myFrame.frame.setPoint(Point.TopLeft, root, Point.Bottom, -210, Hud.bottomFramesTop)
  target.frame.setPoint(Point.TopLeft, root, Point.Bottom, 10, Hud.bottomFramesTop)
  brace.frame.setPoint(Point.TopRight, myFrame.frame, Point.TopLeft, -4, 0)
  private val team = scene.teamIds.indices.map { i =>
    val unit = UnitFrame(root, scene, noVisuals, 150, isTarget = false)
    unit.frame.setPoint(Point.TopLeft, root, Point.TopLeft, 4, 70 + i * (Hud.frameHeight + 4))
    unit
  }

  root.setScript(Script.OnUpdate) { _ =>
    val p = scene.painter
    for (unit, id) <- team.zip(scene.teamIds) do unit.update(p, Some(id))
    myFrame.update(p, scene.myId)
    brace.update(p, scene.myId)
    target.update(p, scene.currentTarget)
    boss.update(p)
    cooldowns.update(p)
    threat.update(p, scene.currentTarget)
    damage.update(p, scene.damageTracker, scene.currentTarget)
    actionBar.update(p)
  }

  private def teamFrame(id: Entity.Id): Frame = team(scene.teamIds.indexOf(id)).frame

  /** What the marks point at (the mark's centre is at that point of the region, offset), and what
    * the legend says about them.
    */
  private def marks: Seq[(Region, Point, Double, Double, String)] = topic match {
    case Topic.Overview =>
      Seq(
        (boss.frame, Point.Left, -16, 0, scene.i18n.legendBoss),
        (cooldowns.frame, Point.Left, -16, 0, scene.i18n.legendBossCooldowns),
        (team.head.frame, Point.TopRight, 6, 6, scene.i18n.legendTeam),
        (myFrame.frame, Point.Left, -16, 0, scene.i18n.legendYouAndTarget),
        (damage.frame, Point.Left, -16, 0, scene.i18n.legendMeters)
      )
    case Topic.Triangle =>
      Seq(
        (myFrame.frame, Point.Left, -16, 0, scene.i18n.legendTriangleEnergy),
        (actionBar.frame, Point.Left, -16, 0, scene.i18n.legendTriangleAbilities),
        (damage.frame, Point.Left, -16, 0, scene.i18n.legendTriangleDamage),
        (threat.frame, Point.Left, -16, 0, scene.i18n.legendTriangleThreat)
      )
    case Topic.Square =>
      Seq(
        (brace.frame, Point.Left, -16, 0, scene.i18n.legendSquareStacks),
        (myFrame.frame, Point.BottomLeft, -10, 2, scene.i18n.legendSquareRage),
        (boss.frame, Point.Left, -16, 0, scene.i18n.legendSquareBigHit),
        (threat.frame, Point.Left, -16, 0, scene.i18n.legendSquareThreat)
      )
    case Topic.Pentagon =>
      Seq(
        (myFrame.frame, Point.Left, -16, 0, scene.i18n.legendPentagonMana),
        (
          actionBar.frame,
          Point.Left,
          -16,
          0,
          scene.i18n.legendPentagonAim
        ),
        (
          teamFrame(scene.idOf(PlayerClasses.Pentagon, 1)),
          Point.TopRight,
          6,
          6,
          scene.i18n.legendPentagonDebuffs
        ),
        (damage.frame, Point.Left, -16, 0, scene.i18n.legendPentagonDamage)
      )
    case Topic.Hexagon =>
      Seq(
        (team.head.frame, Point.TopRight, 6, 6, scene.i18n.legendHexagonTeam),
        (
          teamFrame(scene.idOf(PlayerClasses.Pentagon, 1)),
          Point.TopRight,
          6,
          6,
          scene.i18n.legendHexagonFaded
        ),
        (myFrame.frame, Point.Left, -16, 0, scene.i18n.legendHexagonMana),
        (target.frame, Point.TopRight, 6, 6, scene.i18n.legendHexagonTarget)
      )
  }

  private val placedMarks = marks
  for ((region, point, x, y, _), i) <- placedMarks.zipWithIndex do
    Mark(root, i + 1, topic.colour).setPoint(Point.Center, region, point, x, y)

  /** What the marks are, in order. */
  def legend: Seq[String] = placedMarks.map(_._5)
}

/** Where everyone stands in a fight, with a word on what they do. */
final class ArenaPreview(parent: Frame, area: Region, i18n: I18n) {
  private val canvas = Canvas(parent, area, 780, 460)
  val frame: Frame   = canvas.frame

  private val bossX = canvas.width / 2
  private val bossY = 180.0
  private val dark  = Colour(0.1, 0.1, 0.1)

  // The healer's range (600 units, at the scale of this drawing), around the healer
  private val healerX = bossX - 40
  private val healerY = 390.0
  private val range   = frame.createTexture(Topic.Hexagon.colour.withAlpha(0.08), Layer.Background)
  range.setSize(560, 560)
  range.setPoint(Point.Center, frame, Point.TopLeft, healerX, healerY)
  range.setCornerRadius(280)
  range.setOutline(Some(Topic.Hexagon.colour.withAlpha(0.7)), 2)
  label(i18n.arenaHealerRange, healerX + 150, healerY - 262, Colour.rgb(30, 110, 60))

  Shapes.boss(frame, bossX, bossY, 48)
  label(i18n.arenaBoss, bossX - 34, bossY - 11, dark)

  private def place(topic: Topic, x: Double, y: Double): Unit =
    for cls <- topic.playerClass do Shapes.player(frame, cls, topic.colour, x, y, 40)

  place(Topic.Square, bossX, bossY + 80)
  label(i18n.arenaSquare, bossX + 30, bossY + 70, dark)
  place(Topic.Triangle, bossX + 40, bossY - 78)
  label(i18n.arenaTriangle, bossX + 70, bossY - 90, dark)
  place(Topic.Pentagon, bossX - 250, bossY + 110)
  place(Topic.Pentagon, bossX + 250, bossY + 110)
  label(i18n.arenaPentagons, bossX - 340, bossY + 140, dark)
  place(Topic.Hexagon, healerX, healerY)
  label(i18n.arenaHexagon, healerX + 30, healerY - 11, dark)

  /** On a light background, to be read over the arena (moved to the left when it is too long). */
  private def label(text: String, x: Double, y: Double, colour: Colour): Unit = {
    val fontString = frame.createFontString(text, Theme.body, colour, Layer.Overlay)
    fontString.setPoint(Point.TopLeft, math.min(x, canvas.width - fontString.stringWidth - 8), y)
    val pill = frame.createTexture(Colour(1, 1, 1, 0.75), Layer.Border)
    pill.setPoint(Point.TopLeft, fontString, Point.TopLeft, -5, -2)
    pill.setPoint(Point.BottomRight, fontString, Point.BottomRight, 5, 2)
    pill.setCornerRadius(4)
  }

  def legend: Seq[String] = Seq(
    i18n.arenaLegendPositions,
    i18n.arenaLegendRange
  )
}
