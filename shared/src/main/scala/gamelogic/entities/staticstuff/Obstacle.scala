package gamelogic.entities.staticstuff

import gamelogic.entities.Entity.TeamId
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.{Body, Entity}
import gamelogic.physics.Complex
import gamelogic.physics.shape.Polygon

final case class Obstacle(id: Entity.Id, time: Long, pos: Complex, shape: Polygon) extends Body {
  def rotation: Angle = 0.0

  def teamId: TeamId = Entity.teams.neutralTeam

}
