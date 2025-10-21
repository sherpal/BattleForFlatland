package gamelogic.entities.boss.boss104

import gamelogic.entities.Entity.TeamId
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.classes.Constants
import gamelogic.entities.{Body, Entity}
import gamelogic.physics.Complex
import gamelogic.physics.shape.Circle

case class BigGuyDeathMark(id: Entity.Id, time: Long, pos: Complex) extends Body {
  override def shape: Circle = BigGuyDeathMark.shape

  override def rotation: Angle = 0

  override def teamId: TeamId = Entity.teams.mobTeam
}

object BigGuyDeathMark {
  val radius: Double = Constants.playerRadius

  val shape = Circle(radius)
}
