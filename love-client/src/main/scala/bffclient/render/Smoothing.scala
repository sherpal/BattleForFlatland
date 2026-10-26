package bffclient.render

import gamelogic.entities.{Body, Entity, Moving, WithPosition}
import gamelogic.entities.WithPosition.Angle
import gamelogic.gamestate.GameState
import gamelogic.physics.Complex

/** Smooths the positions shown for the entities that the server moves (other players, bosses, adds,
  * projectiles), without delaying them.
  *
  * Their positions are extrapolated from the last movement received, so they jump when a new
  * movement does not match the extrapolation (a turn or a stop, learnt one round trip late). Such a
  * jump becomes an offset between the shown position and the extrapolated one, which then decays
  * exponentially (halved in `timeConstant * ln 2`). Steady movements have no offset, so they are
  * not delayed. Jumps larger than `snapDistance` (teleports) are shown as they are.
  *
  * Rotations are not extrapolated (they change when the entity turns), so they would snap to each
  * new value: the shown rotation turns towards the actual one (by the shortest way) in the same
  * manner, with `rotationTimeConstant`. Unlike positions, this delays the turns a little.
  *
  * Only for drawing: the game logic (targeting, casting, ...) uses the actual positions.
  */
final class Smoothing(
    timeConstant: Double = 0.1,
    snapDistance: Double = 100,
    rotationTimeConstant: Double = 0.06
) {
  private final case class Smoothed(body: Moving, offset: Complex, rotationOffset: Double)
  private var smoothed = Map.empty[Entity.Id, Smoothed]

  /** Updates the offsets for this frame, `dt` seconds after the previous one. */
  def update(gameState: GameState, now: Long, dt: Double, except: Option[Entity.Id]): Unit = {
    val decay         = math.exp(-dt / timeConstant)
    val rotationDecay = math.exp(-dt / rotationTimeConstant)
    smoothed = gameState.entities.collect {
      case (id, body: Moving) if !except.contains(id) =>
        val (offset, rotationOffset) = smoothed.get(id) match {
          case Some(Smoothed(previous, offset, rotationOffset)) if previous ne body =>
            // Where we showed it (following the previous movement), relative to where it is now
            val jump = offset + previous.currentPosition(now) - body.currentPosition(now)
            val turn =
              Smoothing.shortestAngle(rotationOffset + rotationOf(previous) - rotationOf(body))
            (if jump.modulus > snapDistance then Complex.zero else jump * decay, turn * rotationDecay)
          case Some(Smoothed(_, offset, rotationOffset)) =>
            (offset * decay, rotationOffset * rotationDecay)
          case None => (Complex.zero, 0.0)
        }
        id -> Smoothed(
          body,
          if offset.modulus < 0.01 then Complex.zero else offset,
          if math.abs(rotationOffset) < 0.001 then 0.0 else rotationOffset
        )
    }
  }

  private def rotationOf(entity: Entity): Double = entity match {
    case body: Body => body.rotation.value
    case _ => 0.0
  }

  /** Where to show the entity. */
  def position(entity: WithPosition, now: Long): Complex =
    entity.currentPosition(now) + smoothed.get(entity.id).fold(Complex.zero)(_.offset)

  /** How to show the body turned. */
  def rotation(body: Body): Angle =
    Angle.fromDouble(body.rotation.value + smoothed.get(body.id).fold(0.0)(_.rotationOffset))
}

object Smoothing {

  /** The same angle, between -Pi and Pi. */
  def shortestAngle(angle: Double): Double = {
    val turns = math.floor((angle + math.Pi) / (2 * math.Pi))
    angle - turns * 2 * math.Pi
  }
}
