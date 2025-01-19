package gamelogic.entities.boss.boss102

import gamelogic.buffs.Buff
import gamelogic.buffs.boss.boss102.DamageZoneTick
import gamelogic.entities.Entity.TeamId
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.classes.Constants
import gamelogic.entities.{Body, Entity}
import gamelogic.physics.Complex
import gamelogic.physics.shape.Circle

/** The [[DamageZone]] is put by the boss under each player. It deals damage to all enemies standing
  * in it.
  */
final case class DamageZone(
    id: Entity.Id,
    time: Long,
    pos: Complex,
    radius: Double,
    sourceId: Entity.Id,
    teamId: TeamId
) extends Body {

  lazy val shape: Circle = new Circle(radius)

  def rotation: Angle = 0.0

  def buff(buffId: Buff.Id): DamageZoneTick = DamageZoneTick(buffId, id, time, time)

}

object DamageZone {
  inline def radius: Double = Constants.playerRadius * 4
}
