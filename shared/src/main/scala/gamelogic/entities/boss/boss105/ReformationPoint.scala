package gamelogic.entities.boss.boss105

import gamelogic.entities.Entity.TeamId
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.{Body, Entity}
import gamelogic.physics.Complex
import gamelogic.physics.shape.Circle

/** Marks the spot where the [[Shade]] will come back, so that everyone has time to get ready for
  * it. It stands for as long as the boss is [[gamelogic.buffs.boss.boss105.Mourning]], which is
  * also the delay before the Shade is back.
  */
final case class ReformationPoint(id: Entity.Id, time: Long, pos: Complex) extends Body {
  override def shape: Circle = ReformationPoint.shape

  override def rotation: Angle = Angle.zero

  override def teamId: TeamId = Entity.teams.mobTeam
}

object ReformationPoint {
  val radius: Double = Shade.shape.radius + 10

  val shape: Circle = Circle(radius)
}
