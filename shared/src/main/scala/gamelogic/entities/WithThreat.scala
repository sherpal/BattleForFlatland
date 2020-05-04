package gamelogic.entities

trait WithThreat {

  /**
    * This map remembers the threats that each entity has against this [[WithThreat]].
    */
  val threats: Map[ActionSource, Map[Entity.Id, Double]]

}
