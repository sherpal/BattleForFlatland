package bffclient.hud

import scala.collection.mutable

import bffclient.game.GameClient
import bffclient.gui.*
import bffclient.hud.HudStyle.*
import bffclient.hud.HudWidgets.*
import bffclient.render.Painter
import bffclient.render.bosses.BossVisuals
import bffclient.ui.Colour as Paint
import gamelogic.abilities.{Ability, WithTargetAbility}
import gamelogic.entities.{Entity, EntityCastingInfo, WithPosition}
import gamelogic.entities.boss.BossEntity
import gamelogic.entities.classes.PlayerClass

/** The frontend's in-game UI components, in a frame covering the window with the HUD's scale (the
  * `hudScale` setting: its frames are placed at the edges of a window that much smaller or bigger,
  * which are the window's once scaled): the frames of the team on the left
  * (`PlayerFrameContainer`), our frame, our target's and our cast bar above the action bar
  * (`PlayerFrame`, `TargetFrame`, `PlayerCastBar`) with the stacks of our shield on the left of our
  * frame when we are the tank ([[BraceGauge]]), the boss's frame on top (`BossFrame`), the
  * cooldowns of the boss's abilities on the top right (`BossCooldownsContainer`), the threat meter
  * on the bottom right (`BossThreadMeter`) and above it the damage meter (`DamageMeter`), in the
  * [[HudStyle]].
  *
  * Like in the frontend, clicking the frame of a player or of the boss targets it. The frames show
  * the game state of the frame being drawn (the `painter`'s), in the `OnUpdate` of the HUD's frame.
  *
  * Its [[gui]] also has the dialogs over the game (the game menu, the end of the game), unscaled.
  */
final class Hud(
    client: GameClient,
    bossVisuals: () => Option[BossVisuals],
    painter: () => Option[Painter],
    frameRate: () => String,
    scale: Double = 1.0
) {
  val gui: Gui = Gui()

  /** The HUD's root, at its scale. */
  private val root = Frame(gui.root)
  root.setAllPoints()
  root.setScale(scale)
  private val team      = mutable.ArrayBuffer.empty[UnitFrame]
  private val myFrame   = UnitFrame(root, client, bossVisuals, 200, isTarget = false)
  private val target    = UnitFrame(root, client, bossVisuals, 200, isTarget = true)
  private val brace     = BraceGauge(root)
  private val boss      = BossFrame(root, client)
  private val actionBar = ActionBar(root, client)

  // Our own frame and our target's, left and right of the centre above the action bar
  myFrame.frame.setPoint(Point.TopLeft, root, Point.Bottom, -210, Hud.bottomFramesTop)
  target.frame.setPoint(Point.TopLeft, root, Point.Bottom, 10, Hud.bottomFramesTop)
  // The stacks of our shield when we are the tank, on the left of our frame
  brace.frame.setPoint(Point.TopRight, myFrame.frame, Point.TopLeft, -4, 0)

  private val cooldowns = BossCooldowns(root)
  private val threat    = ThreatMeter(root)
  private val damage    = DamageMeter(root, threat.frame)

  /** What we cast, in the colour of the ability, above our frame and our target's (the frontend's
    * `PlayerCastBar`), with the time remaining.
    */
  private val castBar = panel(root, radius = 5)
  castBar.setSize(200, 22)
  castBar.setPoint(Point.TopLeft, root, Point.Bottom, -100, Hud.bottomFramesTop - 44)
  private val castProgress = HudBar(castBar, 192, 14)
  castProgress.setPoint(Point.TopLeft, castBar, Point.TopLeft, 4, 4)
  castProgress.label.setJustifyH(JustifyH.Center)

  /** A boss-specific line, like the frontend's `bossspecificcomponents`. */
  private val bossLine = panel(root)
  bossLine.setSize(160, 24)
  bossLine.setPoint(Point.Right, root, Point.Right, -4, 0)
  private val bossLineText = textOver(bossLine, bossLine, JustifyH.Right)

  // Before the boss spawns: the "Start game" button where it spawns (like the frontend's), then
  // the countdown, which every player sees
  private val startButton = Button(root, "Start game")
  startButton.setSize(130, 40)
  for (texture, colour) <- Seq(
      startButton.normalTexture    -> startColour,
      startButton.highlightTexture -> startColour.lighter(0.15),
      startButton.pushedTexture    -> startColour.lighter(0.3)
    )
  do {
    texture.setColour(colour)
    texture.setCornerRadius(8)
    texture.setOutline(Some(Paint(0, 0, 0, 0.6)))
  }
  startButton.fontString.setFontSize(largeSize)
  startButton.fontString.setShadow(Some(Paint(0, 0, 0, 0.7)))
  startButton.registerForClicks(1)
  startButton.setScript(Script.OnClick)(_ => client.startCountdown())

  private val hint = panel(root)
  hint.setSize(600, 28)
  hint.setPoint(Point.Top, root, Point.Top, 0, 40)
  textOver(hint, hint, JustifyH.Center)
    .setText("Place yourselves, then click \"Start game\": the boss arrives 5 seconds later")

  private val countdown = panel(root, radius = 10)
  countdown.setSize(260, 130)
  countdown.setPoint(Point.Top, root, Point(0.5, 0.22), 0, -6)
  private val countdownTitle = text(countdown, largeSize, justify = JustifyH.Center)
  place(countdownTitle, countdown, 0, 6, 260, 30)
  countdownTitle.setText("The boss arrives in")
  private val seconds = text(countdown, hugeSize, startColour.lighter(0.4), JustifyH.Center)
  place(seconds, countdown, 0, 36, 260, 90)

  // Ping, messages, ... (the lives are in the frames)
  private def info(colour: Paint, y: Double, justify: JustifyH): FontString = {
    val fontString = root.createFontString(size = normalSize, colour = colour)
    fontString.setJustifyH(justify)
    fontString.setPoint(Point.TopLeft, root, Point.TopLeft, 10, y)
    fontString.setPoint(Point.TopRight, root, Point.TopRight, -10, y)
    fontString
  }
  private val stats       = info(Paint(0, 0, 0), 72, JustifyH.Left)
  private val message     = info(Paint(0, 0, 0), 170, JustifyH.Center)
  private val error       = info(Paint(0.8, 0, 0), 190, JustifyH.Center)
  private val placingZone = info(Paint(0, 0, 0), 210, JustifyH.Center)
  placingZone.setText("Click to place the zone (Escape to cancel)")

  root.setScript(Script.OnUpdate)(_ => painter().foreach(update))

  /** The entity whose frame is under the mouse (for "mouse over" in the controls). */
  def hovered: Option[Entity.Id] =
    gui.hovered.flatMap(frame =>
      (team.iterator ++ Iterator(myFrame, target, boss)).find(_.frame eq frame).flatMap(_.unit)
    )

  def draw(): Unit = gui.draw()

  private def update(p: Painter): Unit = {
    val gameState = p.gameState

    // Players, alive or dead, sorted by name (the frontend's `PlayerFrameContainer`)
    val players = (gameState.players ++ gameState.deadPlayers).toVector.sortBy(_._2.name)
    while team.size < players.size do {
      val frame = UnitFrame(root, client, bossVisuals, 150, isTarget = false)
      frame.frame.setPoint(
        Point.TopLeft,
        root,
        Point.TopLeft,
        4,
        150 + team.size * (Hud.frameHeight + 4)
      )
      team += frame
    }
    for (frame, i) <- team.zipWithIndex do frame.update(p, players.lift(i).map(_._1))
    myFrame.update(p, client.myId)
    brace.update(p, client.myId)
    target.update(p, client.currentTarget)
    boss.update(p)
    cooldowns.update(p)
    threat.update(p, client.currentTarget)
    damage.update(p, client.damageTracker, client.currentTarget)
    actionBar.update(p)

    val casting = for
      id   <- client.myId
      me   <- gameState.players.get(id)
      info <- gameState.castingEntityInfo.get(id) if info.isCasting(p.now, 0L)
    yield (me, info)
    castBar.setShown(casting.isDefined)
    for (me, info) <- casting do {
      castProgress.setValue(Hud.castProgress(p, info))
      castProgress.setColour(softened(info.ability.abilityColour))
      castProgress.label.setText(Hud.abilityName(me, info.ability.abilityId))
      val remaining = math.max(0L, info.remainingCastingTime(p.now, 0L))
      castProgress.value.setText(f"${remaining / 1000.0}%.1f")
    }

    val line = bossVisuals().flatMap(_.hudText(gameState))
    bossLine.setShown(line.isDefined)
    line.foreach(bossLineText.setText)

    val remaining = client.countdownRemaining.filter(_ => client.inPreGame)
    val starting =
      if client.inPreGame && remaining.isEmpty then client.bossStartingPosition else None
    startButton.setShown(starting.isDefined)
    hint.setShown(client.inPreGame && remaining.isEmpty)
    for position <- starting do {
      val (x, y) = p.toWindow(position)
      startButton.setPoint(Point.Center, root, Point.TopLeft, x / scale, y / scale)
    }
    countdown.setShown(remaining.isDefined)
    for ms <- remaining do seconds.setText(math.ceil(ms / 1000.0).toInt.toString)

    stats.setText(
      s"fps: ${frameRate()}   ping: ${client.roundTrip} ms   messages: ${client.messagesReceived}"
    )
    message.setText(
      if gameState.ended then if gameState.playersWon then "Victory!" else "Defeat..."
      else if !gameState.started && !client.inPreGame then "Waiting for the game to start..."
      else ""
    )
    error.setText(client.errorMessage.getOrElse(""))
    placingZone.setShown(client.choosingPosition.isDefined)
  }
}

object Hud {
  val frameHeight = 64.0

  /** Where our frame and our target's are, from the bottom (above the action bar). */
  val bottomFramesTop: Double = -(ActionBar.height + 8 + frameHeight)

  /** 0.5 if the entity is out of our healing range, 1 otherwise. */
  def inHealingRangeAlpha(client: GameClient, p: Painter, entity: Entity & WithPosition): Double =
    (for me <- client.myId.flatMap(p.gameState.players.get) if me.id != entity.id yield {
      val distance2 = (entity.currentPosition(p.now) - me.currentPosition(p.now)).modulus2
      if distance2 > WithTargetAbility.healRangeSquared then 0.5 else 1.0
    }).getOrElse(1.0)

  /** The name of the ability that the entity casts, if known. */
  def abilityName(entity: Entity, abilityId: Ability.AbilityId): String = entity match {
    case boss: BossEntity => boss.abilityNames.getOrElse(abilityId, "")
    case player: PlayerClass =>
      player.cls.builder.abilitiesDoc.find(_.abilityId == abilityId).fold("")(_.name)
    case _ => ""
  }

  def castProgress(p: Painter, info: EntityCastingInfo): Double =
    ratio((p.now - info.startedTime).toDouble, info.castingTime.toDouble)
}
