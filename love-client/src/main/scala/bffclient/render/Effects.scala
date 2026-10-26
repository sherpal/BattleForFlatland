package bffclient.render

import scala.collection.mutable
import scala.util.Random

import bffclient.love.{Graphics, Timer}
import gamelogic.abilities.square.Cleave
import gamelogic.abilities.triangle.DirectHit
import gamelogic.entities.{Body, Entity}
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.classes.Constants
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.gameactions.{EntityGetsHealed, EntityTakesDamage, UseAbility}
import gamelogic.physics.Complex

/** Short visual effects of the game actions, like the frontend's `EffectsManager`: damage and heal
  * numbers, the square's cleave, and the swing of the triangle's direct hit.
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

  private val effects   = mutable.ArrayBuffer.empty[Effect]
  private val seen      = mutable.Set.empty[GameAction.Id]
  private val seenOrder = mutable.Queue.empty[GameAction.Id]
  private lazy val font = Graphics.newFont(16)

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
      val newEffects =
        gameState.applyActionChangers(action).flatMap(effectOf(_, gameState, myId, now))
      if newEffects.nonEmpty && seen.add(action.id) then {
        seenOrder.enqueue(action.id)
        if seenOrder.size > 1000 then seen.remove(seenOrder.dequeue())
        effects ++= newEffects
      }
    }
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
      if t >= 0 && t < effect.duration then effect match {
        case FloatingText(text, (r, g, b), entityId, offset, velocity, _) =>
          for p <- position(entityId) do {
            val (x, y) = toScreen(p + offset + velocity * t)
            val alpha  =
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
                Cleave.cone.vertices.map(v => toScreen(v.rotate(cleave.rotation) + cleave.position))
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
      }
    }
    Graphics.setFont(previousFont)
  }
}
