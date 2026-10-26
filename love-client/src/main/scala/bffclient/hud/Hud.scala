package bffclient.hud

import bffclient.game.GameClient
import bffclient.hud.HudStyle.*
import bffclient.love.{Graphics, Mouse}
import bffclient.render.bosses.BossVisuals
import bffclient.render.{Painter, Sprites}
import bffclient.ui.{Colour as Paint, Rect}
import gamelogic.abilities.{Ability, WithTargetAbility}
import gamelogic.entities.{
  Entity,
  EntityCastingInfo,
  LivingEntity,
  Resource,
  WithAbilities,
  WithName,
  WithPosition
}
import gamelogic.entities.boss.BossEntity
import gamelogic.entities.classes.PlayerClass
import utils.misc.RGBColour

/** The frontend's in-game UI components: the frames of the team on the left
  * (`PlayerFrameContainer`), our frame, our target's and our cast bar above the action bar
  * (`PlayerFrame`, `TargetFrame`, `PlayerCastBar`), the boss's frame on top (`BossFrame`), the
  * cooldowns of the boss's abilities on the top right (`BossCooldownsContainer`) and the threat
  * meter on the bottom right (`BossThreadMeter`). They are drawn in the [[HudStyle]].
  *
  * Like in the frontend, clicking the frame of a player or of the boss targets it.
  */
final class Hud(client: GameClient, bossVisuals: () => Option[BossVisuals], scale: Double = 1.0) {

  /** The frames drawn in the last frame, to target by clicking them. */
  private var clickable = Vector.empty[(Rect, Entity.Id)]

  /** Where the "Start game" button was drawn in the last frame. */
  private var startButton = Option.empty[Rect]

  def startButtonAt(x: Double, y: Double): Boolean = startButton.exists(_.contains(x / scale, y / scale))

  /** The entity whose frame is at this position on screen, if any. */
  def targetAt(x: Double, y: Double): Option[Entity.Id] =
    clickable.collectFirst { case (rect, id) if rect.contains(x / scale, y / scale) => id }

  /** The entity whose frame is under the mouse (for "mouse over" in the controls). */
  def hovered: Option[Entity.Id] = {
    val mouse = Mouse.getPosition()
    targetAt(mouse.x, mouse.y)
  }

  /** Lightens the frame under the mouse (like the frontend's `HoverHighlight`). */
  private def hoverHighlight(rect: Rect, radius: Double = 6): Unit =
    if mouseOver(rect) then {
      roundedFill(rect, radius, Paint(1, 1, 1, 0.1))
      roundedLine(rect, radius, Paint(1, 1, 1, 0.5), 1)
    }

  /** The HUD is drawn at its scale (the mouse is in window pixels). */
  private def mouseOver(rect: Rect): Boolean = {
    val mouse = Mouse.getPosition()
    rect.contains(mouse.x / scale, mouse.y / scale)
  }

  private val frameHeight = 64.0

  private val actionBar = new ActionBar(client, mouseOver)

  /** Where our frame and our target's are (above the action bar). */
  private def bottomFramesTop(height: Double): Double = actionBar.top(height) - 8 - frameHeight

  def draw(p: Painter, width: Double, height: Double): Unit = {
    clickable = Vector.empty
    teamFrames(p)
    myFrame(p, width, height)
    targetFrame(p, width, height)
    castBar(p, width, height)
    bossFrame(p, width)
    bossCooldowns(p, width)
    threatMeter(p, width, height)
    preGame(p, width, height)
    for line <- bossVisuals().flatMap(_.hudText(p.gameState)) do {
      val rect = Rect(width - 164, height / 2 - 12, 160, 24)
      panel(rect)
      text(line, rect, "right")
    }
    actionBar.draw(p.gameState, p.now, width, height) // last: its tooltips are over the rest
  }

  /** Before the boss spawns: the "Start game" button where it spawns (like the frontend's), then
    * the countdown, which every player sees.
    */
  private def preGame(p: Painter, width: Double, height: Double): Unit = {
    startButton = None
    if client.inPreGame then
      client.countdownRemaining match {
        case None =>
          for position <- client.bossStartingPosition do {
            val (x, y) = p.toWindow(position)
            val rect   = Rect(x / scale - 65, y / scale - 20, 130, 40)
            val shade  = if mouseOver(rect) then 0.15 else 0.0
            roundedFill(rect, 8, startColour.lighter(shade))
            roundedLine(rect, 8, Paint(0, 0, 0, 0.6), 1)
            text("Start game", rect, "center", textSize = Some(largeSize))
            startButton = Some(rect)
          }
          val hint = Rect(width / 2 - 300, 40, 600, 28)
          panel(hint)
          text("Place yourselves, then click \"Start game\": the boss arrives 5 seconds later", hint, "center")
        case Some(remaining) =>
          val seconds = math.ceil(remaining / 1000.0).toInt
          panel(Rect(width / 2 - 130, height * 0.22 - 6, 260, 130), radius = 10)
          text("The boss arrives in", Rect(0, height * 0.22, width, 30), "center", textSize = Some(largeSize))
          text(
            seconds.toString,
            Rect(0, height * 0.22 + 30, width, 90),
            "center",
            colour = startColour.lighter(0.4),
            textSize = Some(hugeSize)
          )
      }
  }

  /** Players, alive or dead, sorted by name (the frontend's `PlayerFrameContainer`). */
  private def teamFrames(p: Painter): Unit = {
    val gameState = p.gameState
    val players   = (gameState.players ++ gameState.deadPlayers).toVector.sortBy(_._2.name)
    for ((id, player), i) <- players.zipWithIndex do
      playerFrame(p, id, player, Rect(4, 150 + i * (frameHeight + 4), 150, frameHeight))
  }

  /** Our own frame, left of the centre above the action bar. */
  private def myFrame(p: Painter, width: Double, height: Double): Unit =
    for
      id     <- client.myId
      player <- p.gameState.players.get(id).orElse(p.gameState.deadPlayers.get(id))
    do
      playerFrame(p, id, player, Rect(width / 2 - 210, bottomFramesTop(height), 200, frameHeight))

  /** The class of the player in a round badge, in their colour. */
  private def classBadge(player: PlayerClass, x: Double, y: Double, alpha: Double): Unit = {
    Graphics.setColor(0, 0, 0, 0.4 * alpha)
    Graphics.circle("fill", x, y, 9)
    val (r, g, b) = player.rgb
    Graphics.setColor(r / 255.0, g / 255.0, b / 255.0, alpha)
    Sprites.playerClasses.get(player.cls.value) match {
      case Some(sprite) => sprite.draw(x, y, 0, 14)
      case None => Graphics.circle("fill", x, y, 6)
    }
    // A thin dark border, like the icons'
    Graphics.setColor(0, 0, 0, 0.85 * alpha)
    Graphics.setLineWidth(1)
    Graphics.circle("line", x, y, 9.5)
  }

  /** Class, name, life, resource and buffs of a player (the frontend's `PlayerFrame`). The frames
    * of the players out of our healing range are faded.
    */
  private def playerFrame(p: Painter, id: Entity.Id, player: PlayerClass, frame: Rect): Unit = {
    val gameState = p.gameState
    val alive     = gameState.players.get(id)
    val alpha     = alive.fold(1.0)(inHealingRangeAlpha(p, _))

    panel(frame, alpha)
    for (colour, opacity) <- bossVisuals().flatMap(_.playerFrameTint(gameState, id)) do {
      roundedFill(frame, 6, colour.withAlpha(0.6 * opacity), alpha)
      roundedLine(frame.inset(1), 5, colour, 2, alpha)
    }
    if client.currentTarget.contains(id) then targetGlow(frame)

    classBadge(player, frame.x + 14, frame.y + 12, alpha)
    text(
      player.name,
      Rect(frame.x + 25, frame.y + 3, frame.width - 29, 18),
      colour = if alive.isDefined then textColour else mutedColour,
      alpha = alpha
    )

    val lifeBar = Rect(frame.x + 6, frame.y + 23, frame.width - 12, 14)
    bar(
      lifeBar,
      ratio(alive.fold(0.0)(_.life), player.maxLife),
      lifeColour(ratio(alive.fold(0.0)(_.life), player.maxLife)),
      alpha
    )
    text(
      alive.fold("Dead")(a => s"${math.round(a.life)} / ${math.round(a.maxLife)}"),
      lifeBar,
      "right",
      alpha = alpha
    )

    for a <- alive do resourceBar(a, Rect(frame.x + 6, frame.y + 39, frame.width - 12, 9), alpha)

    val iconSize = 13
    for (
      ((buff, icon), j) <- p
        .iconBuffs(id)
        .zipWithIndex
        .take(((frame.width - 12) / (iconSize + 2)).toInt)
    )
      p.buffIcon(buff, icon, frame.x + 6 + j * (iconSize + 2), frame.y + 50, iconSize)

    hoverHighlight(frame)
    clickable :+= (frame, id)
  }

  /** 0.5 if the entity is out of our healing range, 1 otherwise. */
  private def inHealingRangeAlpha(p: Painter, entity: Entity & WithPosition): Double =
    (for me <- client.myId.flatMap(p.gameState.players.get) if me.id != entity.id yield {
      val distance2 = (entity.currentPosition(p.now) - me.currentPosition(p.now)).modulus2
      if distance2 > WithTargetAbility.healRangeSquared then 0.5 else 1.0
    }).getOrElse(1.0)

  private def resourceBar(entity: WithAbilities, rect: Rect, alpha: Double): Unit = {
    val resource = entity.resourceAmount
    bar(
      rect,
      ratio(resource.amount, entity.maxResourceAmount),
      softened(entity.resourceType.colour),
      alpha
    )
    text(
      s"${math.round(resource.amount)} / ${math.round(entity.maxResourceAmount)}",
      rect,
      "right",
      alpha = alpha
    )
  }

  /** The name of the ability that the entity casts, if known. */
  private def abilityName(entity: Entity, abilityId: Ability.AbilityId): String = entity match {
    case boss: BossEntity => boss.abilityNames.getOrElse(abilityId, "")
    case player: PlayerClass =>
      player.cls.builder.abilitiesDoc.find(_.abilityId == abilityId).fold("")(_.name)
    case _ => ""
  }

  private def castProgress(p: Painter, info: EntityCastingInfo): Double =
    ratio((p.now - info.startedTime).toDouble, info.castingTime.toDouble)

  /** Our target, right of the centre above the action bar (the frontend's `TargetFrame`): name,
    * life, resource and buffs, what it casts just above (in orange), and a lock when we face it
    * (`ToggleTargetLockIn`).
    */
  private def targetFrame(p: Painter, width: Double, height: Double): Unit =
    for (
      case target: (LivingEntity & WithPosition) <- client.currentTarget.flatMap(
        p.gameState.entities.get
      )
    ) {
      val frame = Rect(width / 2 + 10, bottomFramesTop(height), 200, frameHeight)
      val alpha = inHealingRangeAlpha(p, target)
      panel(frame, alpha)

      val nameLeft = target match {
        case player: PlayerClass =>
          classBadge(player, frame.x + 14, frame.y + 12, alpha); frame.x + 25
        case _ => frame.x + 2
      }
      val name = target match {
        case named: WithName => named.name
        case other => s"Entity ${other.id}"
      }
      text(name, Rect(nameLeft, frame.y + 3, frame.right - nameLeft - 4, 18), alpha = alpha)

      val lifeBar   = Rect(frame.x + 6, frame.y + 23, frame.width - 12, 14)
      val lifeRatio = ratio(target.life, target.maxLife)
      bar(lifeBar, lifeRatio, lifeColour(lifeRatio), alpha)
      text(
        s"${math.round(target.life)} / ${math.round(target.maxLife)}",
        lifeBar,
        "right",
        alpha = alpha
      )

      target match {
        case withResource: WithAbilities if withResource.resourceType != Resource.NoResource =>
          resourceBar(withResource, Rect(frame.x + 6, frame.y + 39, frame.width - 12, 9), alpha)
        case _ => ()
      }

      for ((buff, icon), j) <- p.iconBuffs(target.id).zipWithIndex.take(12) do
        p.buffIcon(buff, icon, frame.x + 6 + j * 15, frame.y + 50, 13)

      for info <- p.gameState.castingEntityInfo.get(target.id) if info.isCasting(p.now, 0L) do {
        val castBar = Rect(frame.x, frame.y - 17, frame.width, 13)
        bar(castBar, castProgress(p, info), Paint.rgb(245, 160, 40))
        text(abilityName(target, info.ability.abilityId), castBar, "center")
      }

      if client.lockInToTarget then Sprites.smallLock.foreach { lock =>
        Graphics.setColor(1, 1, 1)
        lock.draw(frame.right - 9, frame.y + 12, 0, lock.frameWidth)
      }
      hoverHighlight(frame)
      clickable :+= (frame, target.id)
    }

  /** What we cast, in the colour of the ability, above our frame and our target's (the frontend's
    * `PlayerCastBar`), with the time remaining.
    */
  private def castBar(p: Painter, width: Double, height: Double): Unit =
    for
      id   <- client.myId
      me   <- p.gameState.players.get(id)
      info <- p.gameState.castingEntityInfo.get(id) if info.isCasting(p.now, 0L)
    do {
      val rect = Rect(width / 2 - 100, bottomFramesTop(height) - 44, 200, 22)
      panel(rect, radius = 5)
      val inner = rect.inset(4)
      bar(inner, castProgress(p, info), softened(info.ability.abilityColour))
      text(abilityName(me, info.ability.abilityId), inner)
      val remaining = math.max(0L, info.remainingCastingTime(p.now, 0L))
      text(f"${remaining / 1000.0}%.1f", inner, "right")
    }

  /** Life and name of the boss, what it casts, and its buffs. */
  private def bossFrame(p: Painter, width: Double): Unit =
    for boss <- p.gameState.bosses.values.headOption do {
      val casting  = p.gameState.castingEntityInfo.get(boss.id).filter(_.isCasting(p.now, 0L))
      val buffs    = p.iconBuffs(boss.id)
      val buffsTop = 4 + (if casting.isDefined then 42 else 26)
      val rect     = Rect(width / 2 - 150, 4, 300, buffsTop - 4 + (if buffs.nonEmpty then 19 else 2))
      panel(rect)
      val lifeBar = Rect(rect.x + 5, rect.y + 5, rect.width - 10, 18)
      val life    = ratio(boss.life, boss.maxLife)
      bar(lifeBar, life, lifeColour(life))
      text(boss.name, lifeBar)
      text(s"${math.round(life * 100)}%", lifeBar, "right")
      for info <- casting do {
        val castBar = Rect(rect.x + 5, rect.y + 26, rect.width - 10, 13)
        bar(castBar, castProgress(p, info), softened(info.ability.abilityColour))
        text(abilityName(boss, info.ability.abilityId), castBar, "center")
      }
      for ((buff, icon), i) <- buffs.zipWithIndex.take(18) do
        p.buffIcon(buff, icon, rect.x + 6 + i * 16, buffsTop, 14)
      hoverHighlight(rect)
      clickable :+= (rect, boss.id)
    }

  /** One bar per ability of the boss, in its colour, emptying until it is ready again. */
  private def bossCooldowns(p: Painter, width: Double): Unit =
    for boss <- p.gameState.bosses.values.headOption do {
      val abilities = boss.abilityNames.toVector
      val rect      = Rect(width - 168, 4, 164, 24 + 17 * abilities.size)
      panel(rect)
      text(
        "Boss abilities",
        Rect(rect.x, rect.y + 3, rect.width, 16),
        colour = mutedColour,
        padding = 6
      )
      for ((abilityId, name), i) <- abilities.zipWithIndex do {
        val row       = Rect(rect.x + 6, rect.y + 21 + 17 * i, rect.width - 12, 14)
        val lastUse   = boss.relevantUsedAbilities.get(abilityId)
        val remaining = lastUse.fold(0L)(use => math.max(0L, use.cooldown - (p.now - use.time)))
        bar(
          row,
          ratio(remaining.toDouble, lastUse.fold(1L)(_.cooldown).toDouble),
          softened(Ability.abilityColour(abilityId))
        )
        text(name, row)
        if remaining > 0 then
          text(
            if remaining >= 1000 then s"${remaining / 1000}s" else f"${remaining / 1000.0}%.1f",
            row,
            "right"
          )
      }
    }

  /** The players by the boss's threat towards them, the highest first. */
  private def threatMeter(p: Painter, width: Double, height: Double): Unit =
    for boss <- p.gameState.bosses.values.headOption do {
      val threats = boss.damageThreats.toVector
        .flatMap((id, amount) => p.gameState.players.get(id).map(_ -> amount.value))
        .sortBy(-_._2)
      val maxThreat = threats.headOption.map(_._2).filter(_ > 0).getOrElse(1.0)
      val rect      = Rect(width - 168, 0, 164, 24 + 17 * threats.size)
      val placed    = rect.copy(y = height - 8 - rect.height)
      panel(placed)
      text(
        "Threat",
        Rect(placed.x, placed.y + 3, placed.width, 16),
        colour = mutedColour,
        padding = 6
      )
      for ((player, amount), i) <- threats.zipWithIndex do {
        val row = Rect(placed.x + 6, placed.y + 21 + 17 * i, placed.width - 12, 14)
        bar(row, amount / maxThreat, softened(RGBColour.fromIntColour(player.colour)))
        text(player.name, row)
      }
    }
}
