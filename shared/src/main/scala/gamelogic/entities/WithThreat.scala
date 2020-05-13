package gamelogic.entities

import gamelogic.entities.WithThreat.ThreatAmount

/**
  * A [[WithThreat]] [[gamelogic.entities.Entity]] remembers the "threat" that each enemy entity poses on itself.
  *
  * Each time an enemy deals damage to this entity, a proportional amount is added to that entity threat. And each time
  * an enemy is healed, the threat posed by their healers also increases.
  *
  * The threat is used by the entity to decide
  */
trait WithThreat {

  /** Maps each entity id to the amount of threat it produced due to healing. */
  def healingThreats: Map[Entity.Id, ThreatAmount]

  /**
    * Maps each entity id to the amount of threat it produced due to damage dealt to this [[gamelogic.entities.Entity]]
    */
  def damageThreats: Map[Entity.Id, ThreatAmount]

}

object WithThreat {

  type ThreatAmount = Double

}
