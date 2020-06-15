package gamelogic.entities.classes.pentagon

import gamelogic.buffs.Buff
import gamelogic.buffs.entities.classes.pentagon.PentagonZoneTick
import gamelogic.entities.Entity.TeamId
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.classes.Constants
import gamelogic.entities.{Body, Entity}
import gamelogic.physics.Complex
import gamelogic.physics.shape.{Polygon, Shape}
import utils.misc.Colour

/**
  * The [[PentagonZone]] is put on the game by an ability of the [[gamelogic.entities.classes.Pentagon]].
  * This zone will stay for a certain amount of time and deal damage over time to enemies standing in it.
  *
  * @param sourceId entity id of the [[gamelogic.entities.classes.Pentagon]] putting the zone
  * @param colour colour for the zone, will be the same as the Hexagon putting it.
  */
final case class PentagonZone(
    id: Entity.Id,
    time: Long,
    pos: Complex,
    rotation: Angle,
    damage: Double,
    sourceId: Entity.Id,
    colour: Colour
) extends Body {
  def shape: Polygon = PentagonZone.shape

  def teamId: TeamId = Entity.teams.playerTeam

  def itsBuff(buffId: Buff.Id): PentagonZoneTick = PentagonZoneTick(buffId, id, time, time, PentagonZone.duration)
}

object PentagonZone {

  final val shape: Polygon = Shape.regularPolygon(5, Constants.playerRadius * 5)

  @inline final def duration: Long       = 5000L
  @inline final def tickRate: Long       = 1000L
  @inline final def damageOnTick: Double = 30.0
}
