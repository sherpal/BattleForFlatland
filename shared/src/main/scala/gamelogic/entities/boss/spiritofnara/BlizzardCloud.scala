package gamelogic.entities.boss.spiritofnara

import gamelogic.entities.Entity
import gamelogic.entities.WithPosition.Angle
import gamelogic.physics.Complex

/** The Blizzard of the Spirit of Nara: a storm going around the room, at
  * [[BlizzardCloud.pathRadius]] from its centre.
  *
  * It moves on its own, at a constant angular speed: where it is only depends on the time, so that
  * nobody needs to tell where it went.
  *
  * @param time
  *   when the storm appeared
  * @param startAngle
  *   where it appeared
  */
final case class BlizzardCloud(id: Entity.Id, time: Long, startAngle: Angle) extends Entity {
  def teamId: Entity.TeamId = Entity.teams.mobTeam

  def centreAt(currentTime: Long): Complex =
    Complex.polar(
      BlizzardCloud.pathRadius,
      startAngle + Angle.fromDouble(BlizzardCloud.angularSpeed * (currentTime - time) / 1000.0)
    )

  def covers(position: Complex, currentTime: Long): Boolean =
    position.distanceTo(centreAt(currentTime)) < BlizzardCloud.radius
}

object BlizzardCloud {
  inline def radius: Double     = 120.0
  inline def pathRadius: Double = 250.0

  /** In radians per second: it goes around the room in about 25 seconds. */
  inline def angularSpeed: Double = 0.25
}
