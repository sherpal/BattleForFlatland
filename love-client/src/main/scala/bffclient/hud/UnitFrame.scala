package bffclient.hud

import bffclient.gui.*
import bffclient.hud.HudStyle.*
import bffclient.hud.HudWidgets.*
import bffclient.render.bosses.BossVisuals
import bffclient.render.{Painter, Sprites}
import bffclient.ui.Colour as Paint
import gamelogic.entities.{Entity, LivingEntity, Resource, WithAbilities, WithName, WithPosition}
import gamelogic.entities.classes.PlayerClass

/** A frame of the HUD that targets its entity when clicked (and is the entity under the mouse, for
  * the "mouse over" casts).
  */
trait UnitButton {
  def frame: Frame
  def unit: Option[Entity.Id]
}

/** The frame of a player, or of our target (the frontend's `PlayerFrame` and `TargetFrame`): class,
  * name, life, resource and buffs, faded when out of our healing range. A player's frame glows when
  * it is our target, and is tinted by the boss's visuals; our target's shows what it casts just
  * above (in orange), and a lock when we face it (`ToggleTargetLockIn`).
  */
final class UnitFrame(
    parent: Frame,
    client: HudClient,
    bossVisuals: () => Option[BossVisuals],
    frameWidth: Double,
    isTarget: Boolean
) extends UnitButton {
  var unit = Option.empty[Entity.Id]

  val frame: Frame = Frame(parent)
  frame.setSize(frameWidth, Hud.frameHeight)
  frame.enableMouse(true)
  // the other buttons can be bound in the controls, e.g. to heal the player under the mouse
  frame.registerForClicks(1)
  frame.setScript(Script.OnClick)(_ => unit.foreach(client.selectTarget))

  // What fades
  private val body = Frame(frame)
  body.setAllPoints()
  body.setBackdrop(panelBackdrop)
  private val tint = body.createTexture(Layer.Background)
  tint.setAllPoints(body)
  tint.setCornerRadius(6)
  private val tintBorder = body.createTexture(Paint(0, 0, 0, 0), Layer.Border)
  tintBorder.setPoint(Point.TopLeft, body, Point.TopLeft, 1, 1)
  tintBorder.setPoint(Point.BottomRight, body, Point.BottomRight, -1, -1)
  tintBorder.setCornerRadius(5)

  private val badge = ClassBadge(body)
  badge.frame.setPoint(Point.Center, body, Point.TopLeft, 14, 12)
  private val name     = text(body)
  private var nameLeft = -1.0
  private val life     = HudBar(body, frameWidth - 12, 14)
  place(life, body, 6, 23, frameWidth - 12, 14)
  private val resource = HudBar(body, frameWidth - 12, 9)
  place(resource, body, 6, 39, frameWidth - 12, 9)
  private val buffs =
    BuffRow(body, 13, 15, if isTarget then 12 else ((frameWidth - 12) / 15).toInt)
  buffs.frame.setPoint(Point.TopLeft, body, Point.TopLeft, 6, 50)

  // Over the rest, whatever its opacity
  private val overlay = Frame(frame)
  overlay.setAllPoints()
  overlay.setFrameLevel(frame.frameLevel + 5)
  private def ring(radius: Double, colour: Paint, width: Double, grow: Double): Texture = {
    val texture = overlay.createTexture(Paint(0, 0, 0, 0), Layer.Artwork)
    texture.setPoint(Point.TopLeft, overlay, Point.TopLeft, -grow, -grow)
    texture.setPoint(Point.BottomRight, overlay, Point.BottomRight, grow, grow)
    texture.setCornerRadius(radius)
    texture.setOutline(Some(colour), width)
    texture
  }
  private val glow = Seq(ring(9, targetColour.withAlpha(0.3), 4, 3), ring(6, targetColour, 2, 0))
  // Lightens the frame under the mouse (like the frontend's `HoverHighlight`)
  private val highlight = ring(6, Paint(1, 1, 1, 0.5), 1, 0)
  highlight.setColour(Paint(1, 1, 1, 0.1))
  private val lock = overlay.createTexture(Layer.Overlay)
  for sprite <- Sprites.smallLock do {
    setSprite(lock, sprite)
    lock.setSize(sprite.frameWidth, sprite.frameHeight)
    lock.setPoint(Point.Center, overlay, Point.TopLeft, frameWidth - 9, 12)
  }
  private val castBar = HudBar(frame, frameWidth, 13)
  castBar.setPoint(Point.TopLeft, frame, Point.TopLeft, 0, -17)
  castBar.setColour(Paint.rgb(245, 160, 40))
  castBar.label.setJustifyH(JustifyH.Center)

  /** Shows the entity (alive, or a dead player), or nothing. */
  def update(p: Painter, id: Option[Entity.Id]): Unit = {
    val gameState = p.gameState
    val living = id.flatMap(gameState.entities.get).collect {
      case entity: (LivingEntity & WithPosition) => entity
    }
    val dead =
      if isTarget || living.isDefined then None else id.flatMap(gameState.deadPlayers.get)
    unit = id.filter(_ => living.isDefined || dead.isDefined)
    frame.setShown(unit.isDefined)
    for
      entityId <- unit
      entity   <- living.orElse(dead)
    do {
      body.setAlpha(living.fold(1.0)(Hud.inHealingRangeAlpha(client, p, _)))

      val tinted =
        if isTarget then None else bossVisuals().flatMap(_.playerFrameTint(gameState, entityId))
      tint.setShown(tinted.isDefined)
      tintBorder.setShown(tinted.isDefined)
      for (colour, opacity) <- tinted do {
        tint.setColour(colour.withAlpha(0.6 * opacity))
        tintBorder.setOutline(Some(colour), 2)
      }
      glow.foreach(_.setShown(!isTarget && client.currentTarget.contains(entityId)))
      highlight.setShown(frame.gui.isHovered(frame))
      lock.setShown(isTarget && client.lockInToTarget)

      val player = entity match {
        case player: PlayerClass => Some(player)
        case _ => None
      }
      badge.frame.setShown(player.isDefined)
      player.foreach(badge.update)
      val left = if player.isDefined then 25.0 else 2.0
      if left != nameLeft then {
        nameLeft = left
        place(name, body, left + 4, 3, frameWidth - left - 12, 18)
      }
      name.setText(entity match {
        case named: WithName => named.name
        case other => s"Entity ${other.id}"
      })
      name.setColour(if living.isDefined then textColour else mutedColour)

      val lifeRatio = living.fold(0.0)(e => ratio(e.life, e.maxLife))
      life.setValue(lifeRatio)
      life.setColour(lifeColour(lifeRatio))
      life.value.setText(
        living.fold("Dead")(e => s"${math.round(e.life)} / ${math.round(e.maxLife)}")
      )

      val withResource = living.collect {
        case e: WithAbilities if e.resourceType != Resource.NoResource => e
      }
      resource.setShown(withResource.isDefined)
      for e <- withResource do {
        val amount = e.resourceAmount.amount
        resource.setValue(ratio(amount, e.maxResourceAmount))
        resource.setColour(softened(e.resourceType.colour))
        resource.value.setText(s"${math.round(amount)} / ${math.round(e.maxResourceAmount)}")
      }

      buffs.update(p.iconBuffs(entityId), gameState.time)

      val casting =
        if isTarget then gameState.castingEntityInfo.get(entityId).filter(_.isCasting(p.now, 0L))
        else None
      castBar.setShown(casting.isDefined)
      for info <- casting do {
        castBar.setValue(Hud.castProgress(p, info))
        castBar.label.setText(Hud.abilityName(entity, info.ability.abilityId))
      }
    }
  }
}
