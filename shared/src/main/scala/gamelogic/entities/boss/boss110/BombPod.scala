package gamelogic.entities.boss.boss110

import gamelogic.entities.Entity.TeamId
import gamelogic.entities.classes.Constants
import gamelogic.entities.{Body, Entity}
import gamelogic.physics.Complex
import gamelogic.physics.shape.Circle
import gamelogic.physics.shape.Shape
import gamelogic.entities.WithPosition

/**
  * Entity representing a bomb placed by the Boss.
  *
  * @param id id of the bomb
  * @param time time at which the bomb appeared
  * @param pos position of the bomb
  * @param powderMonkeyId entity id of the entity who placed the bomb
  */
final case class BombPod(
    id: Entity.Id,
    time: Long,
    pos: Complex,
    powderMonkeyId: Entity.Id
) extends Body {

  def teamId: Entity.TeamId = Entity.teams.mobTeam

  def shape: Shape = BombPod.shape

  def rotation: WithPosition.Angle = 0

}

object BombPod {
  def shape: Circle = new Circle(15.0)
}
