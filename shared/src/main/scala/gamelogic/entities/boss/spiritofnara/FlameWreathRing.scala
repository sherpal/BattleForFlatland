package gamelogic.entities.boss.spiritofnara

import gamelogic.entities.{Body, Entity}
import gamelogic.entities.WithPosition.Angle
import gamelogic.physics.Complex
import gamelogic.physics.shape.Circle

/** A ring of fire put by the Flame Wreath of the Spirit of Nara.
  *
  * Whoever is inside must stay inside, whoever is outside must stay outside: as soon as somebody
  * crosses a ring, they all explode (see [[gamelogic.buffs.boss.spiritofnara.FlameWreathWatch]]).
  *
  * @param insiderIds
  *   the players who were inside the ring when it appeared
  */
final case class FlameWreathRing(
    id: Entity.Id,
    time: Long,
    pos: Complex,
    insiderIds: Vector[Entity.Id]
) extends Body {
  def rotation: Angle = Angle.zero

  def shape: Circle = FlameWreathRing.shape

  def teamId: Entity.TeamId = Entity.teams.mobTeam

  def isInside(position: Complex): Boolean = position.distanceTo(pos) < FlameWreathRing.radius

  /** Whether the player at that position is not on the side of the ring where it started. */
  def isCrossedBy(playerId: Entity.Id, position: Complex): Boolean =
    isInside(position) != insiderIds.contains(playerId)
}

object FlameWreathRing {
  inline def radius: Double = 75.0

  val shape: Circle = Circle(radius)
}
