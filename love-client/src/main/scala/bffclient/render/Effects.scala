package bffclient.render

import scala.collection.mutable
import scala.util.Random

import bffclient.controls.ClientSettings
import bffclient.hud.{BraceGauge, FacetsGauge}
import bffclient.love.{Graphics, Source, Timer}
import bffclient.ui.Colour
import gamelogic.abilities.pentagon.Pentagram
import gamelogic.abilities.square.{Brace, Cleave, Guard, ShieldSlam}
import gamelogic.abilities.triangle.DirectHit
import gamelogic.entities.{Body, Entity}
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.classes.Constants
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.gameactions.{EntityGetsHealed, EntityTakesDamage, UseAbility}
import gamelogic.physics.Complex

/** Short visual effects of the game actions, like the frontend's `EffectsManager`: damage and heal
  * numbers, the square's cleave, brace, shield slam and guard, the swing of the triangle's direct
  * hit, and the pentagon's pentagram. Also plays the [[Sounds]] of the abilities (louder for ours),
  * at the volume of the settings.
  *
  * Unlike the frontend, the numbers follow their entity (from where it is shown: predicted for our
  * player, smoothed for the others) instead of staying where they started.
  */
final class Effects {
  private sealed trait Effect {
    def start: Double
    def duration: Double
  }

  /** A number above `entityId` (`offset` from its position), moving at `velocity` per second. */
  private final case class FloatingText(
      text: String,
      colour: (Double, Double, Double),
      entityId: Entity.Id,
      offset: Complex,
      velocity: Complex,
      start: Double
  ) extends Effect {
    def duration = 2.0
  }

  /** The cleave's sprite at `position`, or (without it) the cone that it hits. */
  private final case class CleaveFlash(
      position: Complex,
      rotation: Angle,
      cleave: Cleave,
      start: Double
  ) extends Effect {
    def duration = 0.75
  }

  /** The sword turns around the caster, from one side of the direction of the target to the other.
    */
  private final case class DirectHitSwing(casterId: Entity.Id, towardsTarget: Angle, start: Double)
      extends Effect {
    def duration = 0.2
  }

  /** The square's shield closes around it, in the colour of its stacks (see [[BraceGauge]]). */
  private final case class BraceFlash(casterId: Entity.Id, radius: Double, start: Double)
      extends Effect {
    def duration = 0.4
  }

  /** The square's shield is thrust towards its target, which it hits with a shock wave. */
  private final case class ShieldSlamThrust(
      casterId: Entity.Id,
      targetId: Entity.Id,
      casterRadius: Double,
      targetRadius: Double,
      start: Double
  ) extends Effect {
    def duration = 0.4
  }

  /** A link from the square to the ally that it guards, around which a ring closes. */
  private final case class GuardLink(
      casterId: Entity.Id,
      targetId: Entity.Id,
      targetRadius: Double,
      start: Double
  ) extends Effect {
    def duration = 0.6
  }

  /** The five facets of the pentagon leave it, and draw a star turning on its target. */
  private final case class PentagramStar(
      casterId: Entity.Id,
      targetId: Entity.Id,
      targetRadius: Double,
      start: Double
  ) extends Effect {
    def duration = 0.6
  }

  private val effects   = mutable.ArrayBuffer.empty[Effect]
  private val seen      = mutable.Set.empty[GameAction.Id]
  private val seenOrder = mutable.Queue.empty[GameAction.Id]
  private lazy val font = Graphics.newFont(16)
  // Loaded now rather than when an ability is first used
  private val sounds = Sounds.abilities

  /** Actions received from the server, already in `gameState`. When the server corrects the past,
    * it sends actions again: each one has its effects once.
    */
  def received(
      actions: Iterable[GameAction],
      gameState: GameState,
      myId: Option[Entity.Id]
  ): Unit = {
    val now = Timer.getTime()
    effects.filterInPlace(effect => now - effect.start < effect.duration)
    for action <- actions do {
      val changed    = gameState.applyActionChangers(action)
      val newEffects = changed.flatMap(effectOf(_, gameState, myId, now))
      val newSounds  = changed.flatMap(soundOf(_, gameState, myId))
      if (newEffects.nonEmpty || newSounds.nonEmpty) && seen.add(action.id) then {
        seenOrder.enqueue(action.id)
        if seenOrder.size > 1000 then seen.remove(seenOrder.dequeue())
        effects ++= newEffects
        for (sound, volume) <- newSounds do Sounds.play(sound, volume)
      }
    }
  }

  /** The sound of an ability that was just used (not one of the past, e.g. when we join). */
  private def soundOf(
      action: GameAction,
      gameState: GameState,
      myId: Option[Entity.Id]
  ): Option[(Source, Double)] = action match {
    case UseAbility(_, time, casterId, _, ability) if gameState.time - time < 1000 =>
      val volume = if myId.contains(casterId) then 0.7 else 0.25
      sounds.get(ability.abilityId).map(_ -> volume * ClientSettings.current.volumes.ofAbilities)
    case _ => None
  }

  private def effectOf(
      action: GameAction,
      gameState: GameState,
      myId: Option[Entity.Id],
      now: Double
  ): Option[Effect] = {
    def isMe(id: Entity.Id) = myId.contains(id)
    // A little randomness, so that successive numbers do not overlap
    def jitter(z: Complex) =
      z * Complex.rotation(Angle.fromDouble(Random.between(-math.Pi / 16, math.Pi / 16)))
    def radius(id: Entity.Id) =
      gameState.entities.get(id).collect { case body: Body => body.shape.radius }.getOrElse(0.0)
    def amountText(amount: Double) = math.round(amount).toString
    action match {
      case EntityTakesDamage(_, _, entityId, amount, sourceId)
          if isMe(entityId) && !isMe(sourceId) =>
        Some(
          FloatingText(
            amountText(amount),
            (0.9, 0.1, 0.1),
            entityId,
            Complex.zero,
            jitter(-40 * Complex.i),
            now
          )
        )
      case EntityTakesDamage(_, _, entityId, amount, sourceId) if isMe(sourceId) =>
        Some(
          FloatingText(
            amountText(amount),
            (1, 1, 1),
            entityId,
            radius(entityId) * Complex.i,
            jitter(40 * Complex.i),
            now
          )
        )
      case EntityGetsHealed(_, _, entityId, amount, sourceId) if isMe(sourceId) =>
        Some(
          FloatingText(
            amountText(amount),
            (0.1, 0.8, 0.1),
            entityId,
            Complex.zero,
            jitter(40 * Complex.i),
            now
          )
        )
      case UseAbility(_, _, casterId, _, cleave: Cleave) =>
        gameState.players.get(casterId).map { player =>
          CleaveFlash(
            cleave.position + player.shape.radius * Complex.rotation(cleave.rotation),
            Angle.Pi + cleave.rotation,
            cleave,
            now
          )
        }
      case UseAbility(_, _, casterId, _, hit: DirectHit) =>
        for
          caster <- gameState.players.get(casterId)
          case target: Body <- gameState.entities.get(hit.targetId)
        yield DirectHitSwing(casterId, (target.pos - caster.pos).arg, now)
      case UseAbility(_, _, casterId, _, _: Brace) =>
        Some(BraceFlash(casterId, radius(casterId), now))
      case UseAbility(_, _, casterId, _, slam: ShieldSlam) =>
        Some(
          ShieldSlamThrust(casterId, slam.targetId, radius(casterId), radius(slam.targetId), now)
        )
      case UseAbility(_, _, casterId, _, guard: Guard) =>
        Some(GuardLink(casterId, guard.targetId, radius(guard.targetId), now))
      case UseAbility(_, _, casterId, _, pentagram: Pentagram) =>
        Some(PentagramStar(casterId, pentagram.targetId, radius(pentagram.targetId), now))
      case _ =>
        None
    }
  }

  /** Draws the current effects, with where entities are shown and the world to screen transform. */
  def draw(position: Entity.Id => Option[Complex], toScreen: Complex => (Double, Double)): Unit = {
    val now          = Timer.getTime()
    val previousFont = Graphics.getFont()
    Graphics.setFont(font)
    for effect <- effects do {
      val t = now - effect.start
      if t >= 0 && t < effect.duration then
        effect match {
          case FloatingText(text, (r, g, b), entityId, offset, velocity, _) =>
            for p <- position(entityId) do {
              val (x, y) = toScreen(p + offset + velocity * t)
              val alpha =
                math.min(1.0, 4 * (effect.duration - t)) // fades out in the last quarter second
              Graphics.setColor(0, 0, 0, alpha * 0.8)
              Graphics.printf(text, x - 49, y - 9, 100, "center")
              Graphics.setColor(r, g, b, alpha)
              Graphics.printf(text, x - 50, y - 10, 100, "center")
            }
          case CleaveFlash(p, rotation, cleave, _) =>
            val (x, y) = toScreen(p)
            Sprites.cleave match {
              case Some(sprite) =>
                Graphics.setColor(1, 1, 1)
                val size = Cleave.cone.radius
                sprite.draw(
                  x,
                  y,
                  rotation.value,
                  size,
                  size,
                  frame = (t / effect.duration * sprite.frameCount).toInt
                )
              case None =>
                val corners =
                  Cleave.cone.vertices.map(v =>
                    toScreen(v.rotate(cleave.rotation) + cleave.position)
                  )
                Graphics.setColor(1, 1, 1, 0.6 * (1 - t / effect.duration))
                Graphics.polygon("fill", corners.flatMap((cx, cy) => Seq(cx, cy))*)
            }
          case DirectHitSwing(casterId, towardsTarget, _) =>
            for caster <- position(casterId) do {
              val angle =
                towardsTarget - Angle.Pi / 4 + Angle.fromDouble(math.Pi / 2 * t / effect.duration)
              val offset = 1.5 * Constants.playerRadius * Complex.rotation(angle)
              val (x, y) = toScreen(caster + offset)
              Graphics.setColor(1, 1, 1)
              Sprites.sword match {
                case Some(sprite) => sprite.draw(x, y, angle.value, sprite.frameWidth)
                case None =>
                  val (x1, y1) = toScreen(caster + offset * 1.6)
                  Graphics.setLineWidth(2)
                  Graphics.line(x, y, x1, y1)
                  Graphics.setLineWidth(1)
              }
            }
          case BraceFlash(casterId, radius, _) =>
            for caster <- position(casterId) do {
              val progress = t / effect.duration
              val (x, y)   = toScreen(caster)
              val ring     = radius * (2.2 - 1.0 * (1 - math.pow(1 - progress, 3)))
              setColour(BraceGauge.colour, 0.25 * (1 - progress))
              Graphics.circle("fill", x, y, ring)
              setColour(BraceGauge.fullColour, math.min(1.0, 4 * (1 - progress)))
              Graphics.setLineWidth(3)
              Graphics.circle("line", x, y, ring)
              Graphics.setLineWidth(1)
            }
          case ShieldSlamThrust(casterId, targetId, casterRadius, targetRadius, _) =>
            for
              caster <- position(casterId)
              target <- position(targetId)
            do {
              val progress  = t / effect.duration
              val direction = (target - caster).safeNormalized
              val from      = caster + 1.2 * casterRadius * direction
              val to        = target - targetRadius * direction
              // The shield goes to the target, then a shock wave spreads where it hits
              val thrust   = math.min(1.0, progress / 0.4)
              val shield   = from + (to - from) * (thrust * thrust)
              val side     = casterRadius * direction.orthogonal
              val (x1, y1) = toScreen(shield + side)
              val (x2, y2) = toScreen(shield - side)
              setColour(BraceGauge.fullColour, if progress < 0.4 then 1.0 else 2 * (1 - progress))
              Graphics.setLineWidth(6)
              Graphics.line(x1, y1, x2, y2)
              if progress >= 0.4 then {
                val wave   = (progress - 0.4) / 0.6
                val (x, y) = toScreen(to)
                Graphics.setColor(1, 1, 1, 1 - wave)
                Graphics.setLineWidth(3)
                Graphics.circle("line", x, y, 4 + 1.6 * targetRadius * wave)
              }
              Graphics.setLineWidth(1)
            }
          case GuardLink(casterId, targetId, targetRadius, _) =>
            for
              caster <- position(casterId)
              target <- position(targetId)
            do {
              val progress  = t / effect.duration
              val (cx, cy)  = toScreen(caster)
              val (tx, ty)  = toScreen(target)
              val (r, g, b) = Effects.guardColour
              Graphics.setColor(r, g, b, 0.6 * (1 - progress))
              Graphics.setLineWidth(2)
              Graphics.line(cx, cy, tx, ty)
              // A spark runs along the link, then a ring closes around the ally
              if progress < 0.5 then {
                val (sx, sy) = toScreen(caster + (target - caster) * (progress / 0.5))
                Graphics.setColor(r, g, b, 1)
                Graphics.circle("fill", sx, sy, 4)
              }
              Graphics.setColor(r, g, b, math.min(1.0, 3 * (1 - progress)))
              Graphics.setLineWidth(3)
              Graphics.circle("line", tx, ty, targetRadius * (2 - 0.7 * progress))
              Graphics.setLineWidth(1)
            }
          case PentagramStar(casterId, targetId, targetRadius, _) =>
            for
              caster <- position(casterId)
              target <- position(targetId)
            do {
              val progress = t / effect.duration
              // The five sparks fly to the target in the first third, then the star shines
              val flight = math.min(1.0, progress * 3)
              val spin   = Angle.fromDouble(progress * math.Pi / 2)
              val points = (0 until 5).map(k =>
                Complex.rotation(spin + Angle.fromDouble(math.Pi / 2 + 2 * math.Pi * k / 5))
              )
              val radius = targetRadius * 1.4
              if flight < 1 then
                for point <- points do {
                  val (x, y) = toScreen(caster + (target + radius * point - caster) * flight)
                  setColour(FacetsGauge.fullColour, 1)
                  Graphics.circle("fill", x, y, 4)
                }
              else {
                // Every second point, to draw the star
                val corners = (0 until 5).map(k => toScreen(target + radius * points(2 * k % 5)))
                setColour(FacetsGauge.fullColour, math.min(1.0, 3 * (1 - progress)))
                Graphics.setLineWidth(3)
                for k <- 0 until 5 do {
                  val (x1, y1) = corners(k)
                  val (x2, y2) = corners((k + 1) % 5)
                  Graphics.line(x1, y1, x2, y2)
                }
                Graphics.setLineWidth(1)
              }
            }
        }
    }
    Graphics.setFont(previousFont)
  }

  private def setColour(colour: Colour, alpha: Double): Unit =
    Graphics.setColor(colour.r, colour.g, colour.b, colour.a * alpha)
}

object Effects {

  /** Golden, for the protection of an ally. */
  val guardColour: (Double, Double, Double) = (0.95, 0.8, 0.35)
}
