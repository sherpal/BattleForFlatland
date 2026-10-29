package bffclient.render

import bffclient.hud.HudStyle
import bffclient.love.Graphics
import bffclient.ui.{Colour as Paint, Rect}
import gamelogic.buffs.Buff
import gamelogic.entities.{Body, Entity, LivingEntity, WithPosition}
import gamelogic.entities.WithPosition.Angle
import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import gamelogic.physics.shape.{Circle, Polygon}

/** Drawing in world coordinates, for one frame: the camera at `cameraPosition` (y axis pointing
  * up), entities where they are shown (smoothed, except our predicted player).
  */
final class Painter(
    val gameState: GameState,
    val now: Long,
    cameraPosition: Complex,
    screenWidth: Double,
    screenHeight: Double,
    smoothing: Smoothing,
    /** Window pixels per screen pixel (the world is drawn scaled, see [[Renderer]]). */
    val scale: Double = 1.0
) {

  def position(entity: WithPosition): Complex = smoothing.position(entity, now)
  def rotation(body: Body): Angle             = smoothing.rotation(body)

  def toScreen(z: Complex): (Double, Double) = {
    val d = z - cameraPosition
    (screenWidth / 2.0 + d.re, screenHeight / 2.0 - d.im)
  }

  /** Where the point is in the window (e.g. to draw the HUD there, which is not scaled). */
  def toWindow(z: Complex): (Double, Double) = {
    val (x, y) = toScreen(z)
    (x * scale, y * scale)
  }

  def polygonCoords(points: Iterable[Complex]): Seq[Double] =
    points.toSeq.flatMap { p =>
      val (x, y) = toScreen(p); Seq(x, y)
    }

  def setColour(colour: Paint): Unit = Graphics.setColor(colour.r, colour.g, colour.b, colour.a)

  /** Draws the body's shape with the current colour, "fill" or "line". */
  def drawShape(body: Body, mode: String): Unit = {
    val pos = position(body)
    body.shape match {
      case circle: Circle =>
        val (x, y) = toScreen(pos)
        Graphics.circle(mode, x, y, circle.radius)
      case polygon: Polygon if mode == "fill" =>
        // Fill triangle by triangle, as obstacles can be concave. The corners are plain arguments:
        // spread as varargs (once per triangle, every frame), LÖVE sometimes received an odd
        // number of coordinates.
        val angle = rotation(body)
        def corner(x: Double, y: Double): (Double, Double) =
          toScreen(Complex(x, y).rotate(angle) + pos)
        for t <- polygon.triangulation do {
          val (x0, y0) = corner(t.x0, t.y0)
          val (x1, y1) = corner(t.x1, t.y1)
          val (x2, y2) = corner(t.x2, t.y2)
          Graphics.triangle("fill", x0, y0, x1, y1, x2, y2)
        }
      case polygon: Polygon =>
        Graphics.polygon(
          "line",
          polygonCoords(polygon.translateAndRotationVertices(pos, rotation(body)))*
        )
      case _ =>
        ()
    }
  }

  /** The body's shape, filled and/or outlined (stroke: width and colour). */
  def shape(body: Body, fill: Option[Paint], stroke: Option[(Double, Paint)] = None): Unit = {
    fill.foreach { colour =>
      setColour(colour); drawShape(body, "fill")
    }
    stroke.foreach { (width, colour) =>
      setColour(colour); Graphics.setLineWidth(width); drawShape(body, "line");
      Graphics.setLineWidth(1)
    }
  }

  def circle(
      centre: Complex,
      radius: Double,
      fill: Option[Paint],
      stroke: Option[(Double, Paint)] = None
  ): Unit = {
    val (x, y) = toScreen(centre)
    fill.foreach { colour =>
      setColour(colour); Graphics.circle("fill", x, y, radius)
    }
    stroke.foreach { (width, colour) =>
      setColour(colour); Graphics.setLineWidth(width); Graphics.circle("line", x, y, radius);
      Graphics.setLineWidth(1)
    }
  }

  /** A convex polygon. */
  def polygon(
      points: Seq[Complex],
      fill: Option[Paint],
      stroke: Option[(Double, Paint)] = None
  ): Unit = {
    val coords = polygonCoords(points)
    fill.foreach { colour =>
      setColour(colour); Graphics.polygon("fill", coords*)
    }
    stroke.foreach { (width, colour) =>
      setColour(colour); Graphics.setLineWidth(width); Graphics.polygon("line", coords*);
      Graphics.setLineWidth(1)
    }
  }

  def line(from: Complex, to: Complex, width: Double, colour: Paint): Unit = {
    val (x0, y0) = toScreen(from)
    val (x1, y1) = toScreen(to)
    setColour(colour)
    Graphics.setLineWidth(width)
    Graphics.line(x0, y0, x1, y1)
    Graphics.setLineWidth(1)
  }

  /** A line from the centre of the body in the direction it faces. */
  def nose(body: Body, colour: (Double, Double, Double)): Unit = {
    val pos = position(body)
    line(
      pos,
      pos + Complex.polar(body.shape.radius, rotation(body)),
      2,
      Paint(colour._1, colour._2, colour._3)
    )
  }

  /** Above the entity, in the [[HudStyle]]. */
  def lifeBar(entity: LivingEntity & Body): Unit = {
    val radius = entity.shape.radius
    val (x, y) = toScreen(position(entity) + Complex.i * (radius + 10))
    val ratio  = HudStyle.ratio(entity.life, entity.maxLife)
    HudStyle.bar(Rect(x - radius, y - 3, 2 * radius, 6), ratio, HudStyle.lifeColour(ratio))
  }

  /** The buffs of the entity that have an icon, the oldest first (like the frontend's buff
    * containers).
    */
  def iconBuffs(entityId: Entity.Id): Seq[(Buff, Sprite)] =
    gameState
      .allBuffsOfEntity(entityId)
      .toSeq
      .sortBy(_.appearanceTime)
      .flatMap(buff => Sprites.buffIcons.get(buff.resourceIdentifier).map(buff -> _))

  /** A row of the entity's buff icons above its life bar. Like the frontend, the icons of the buffs
    * that end are darkened from the top, by the part of their duration that remains.
    */
  def buffIcons(entity: Entity & Body): Unit = {
    val buffs = iconBuffs(entity.id)
    if buffs.nonEmpty then {
      val size   = 16.0
      val (x, y) = toScreen(position(entity) + Complex.i * (entity.shape.radius + 10))
      val left   = x - buffs.size * (size + 2) / 2
      val top    = y - 2.5 - 3 - size
      for ((buff, icon), i) <- buffs.zipWithIndex do
        buffIcon(buff, icon, left + i * (size + 2) + 1, top, size)
    }
  }

  /** The icon of the buff (on screen), darkened from the top by the part of its duration that
    * remains.
    */
  def buffIcon(buff: Buff, icon: Sprite, left: Double, top: Double, size: Double): Unit = {
    Graphics.setColor(1, 1, 1)
    icon.draw(left + size / 2, top + size / 2, 0, size, size)
    if buff.isFinite then {
      Graphics.setColor(0, 0, 0, 0.5)
      Graphics.rectangle("fill", left, top, size, size * buff.remainingPercentage(gameState.time))
    }
    // A thin dark border, like the ability icons'
    HudStyle.roundedLine(Rect(left - 1, top - 1, size + 2, size + 2), 2, Paint(0, 0, 0, 0.85), 1)
  }

  /** Below the entity, in the colour of the ability, in the [[HudStyle]]. */
  def castingBar(entity: Entity & Body): Unit =
    gameState.castingEntityInfo.get(entity.id).filter(_.isCasting(now, 0L)).foreach { info =>
      val radius = entity.shape.radius
      val (x, y) = toScreen(position(entity) - Complex.i * (radius + 10))
      val progress =
        (info.castingTime - info.remainingCastingTime(now, 0L)).toDouble / info.castingTime
      HudStyle.bar(
        Rect(x - radius, y - 3, 2 * radius, 6),
        progress,
        HudStyle.softened(info.ability.abilityColour)
      )
    }
}
