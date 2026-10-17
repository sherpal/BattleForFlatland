package gamelogic.entities.boss.sirofvice

import gamelogic.entities.Entity
import gamelogic.physics.Complex

/** Purely visual: the path followed by the last [[gamelogic.abilities.boss.sirofvice.HolyWrath]],
  * from the boss to the last player hit. Each new wrath replaces the previous chain, and the
  * frontend only draws it during [[HolyWrathChain.displayDuration]].
  */
final case class HolyWrathChain(id: Entity.Id, time: Long, positions: Vector[Complex])
    extends Entity {
  def teamId: Entity.TeamId = Entity.teams.mobTeam
}

object HolyWrathChain {
  inline def displayDuration: Long = 800L
}
