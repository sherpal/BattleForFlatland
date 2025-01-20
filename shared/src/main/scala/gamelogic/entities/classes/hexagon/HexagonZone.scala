package gamelogic.entities.classes.hexagon

import gamelogic.entities.Entity
import gamelogic.physics.Complex
import gamelogic.entities.WithPosition.Angle
import utils.misc.Colour
import gamelogic.entities.Body
import gamelogic.physics.shape.Polygon
import gamelogic.physics.shape.Shape
import gamelogic.entities.classes.Constants
import gamelogic.entities.Entity.TeamId
import gamelogic.buffs.Buff
import gamelogic.buffs.entities.classes.hexagon.HexagonZoneTick

final case class HexagonZone(
    id: Entity.Id,
    time: Long,
    pos: Complex,
    rotation: Angle,
    heal: Double,
    sourceId: Entity.Id,
    colour: Colour
) extends Body {
  inline def shape: Polygon = HexagonZone.shape

  def teamId: TeamId = Entity.teams.playerTeam

  def itsBuff(buffId: Buff.Id): HexagonZoneTick =
    HexagonZoneTick(buffId, id, time, time, HexagonZone.duration)

}

object HexagonZone {

  val shape: Polygon = Shape.regularPolygon(6, Constants.playerRadius * 3)

  inline def duration: Long     = 5000L
  inline def tickRate: Long     = 1000L
  inline def healOnTick: Double = 10.0 // ?

}
