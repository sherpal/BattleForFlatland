package gamelogic.entities

import gamelogic.entities.WithThreat.ThreatAmount

/**
  * A [[WithThreat]] [[gamelogic.entities.Entity]] remembers the "threat" that each enemy entity poses on itself.
  *
  * Each time an enemy deals damage to this entity, a proportional amount is added to that entity threat. And each time
  * an enemy is healed, the threat posed by their healers also increases.
  *
  * The threat is used by the entity to decide who to attack in some situations.
  *
  * Typically, the main boss in a fight will either attack the entity at the top of the damage threats or the heal at
  * the top of the healing threats.
  */
trait WithThreat extends Entity {

  /** Maps each entity id to the amount of threat it produced due to healing. */
  def healingThreats: Map[Entity.Id, ThreatAmount]

  /**
    * Maps each entity id to the amount of threat it produced due to damage dealt to this [[gamelogic.entities.Entity]]
    */
  def damageThreats: Map[Entity.Id, ThreatAmount]

  /** Concrete classes must implement this so that we can change the damage threat of the entity. */
  def changeDamageThreats(threatId: Entity.Id, delta: ThreatAmount): WithThreat

  /** Concrete classes must implement this so that we can change the healing threat of the entity. */
  def changeHealingThreats(threatId: Entity.Id, delta: ThreatAmount): WithThreat

  /** Calls either of the changing threats function depending on the value of `isDamageThreat`. */
  final def changeThreats(threatId: Entity.Id, delta: ThreatAmount, isDamageThreat: Boolean): WithThreat =
    if (isDamageThreat) changeDamageThreats(threatId, delta) else changeHealingThreats(threatId, delta)

}

object WithThreat {

  type ThreatAmount = Double

}
