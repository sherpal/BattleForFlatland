package gamelogic.entities.boss.boss104

import gamelogic.entities.classes.Constants
import gamelogic.physics.shape.Circle
import gamelogic.entities.Body
import gamelogic.entities.Entity
import gamelogic.physics.Complex
import gamelogic.entities.Entity.TeamId
import gamelogic.physics.shape.Shape
import gamelogic.entities.WithPosition.Angle
import utils.misc.RGBColour

final case class DebuffCircle(
    id: Entity.Id,
    time: Long,
    pos: Complex,
    colour: RGBColour
) extends Body {

  override def rotation: Angle = 0

  override def shape: Shape = DebuffCircle.shape

  override def teamId: TeamId = Entity.teams.mobTeam

}

object DebuffCircle {
  inline def radius: Double = Constants.playerRadius * 3

  val shape = Circle(radius)
}
