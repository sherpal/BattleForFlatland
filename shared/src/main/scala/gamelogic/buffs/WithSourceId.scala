package gamelogic.buffs

import gamelogic.entities.Entity

/** A [[Buff]] that an entity, `sourceId`, put on its bearer. */
trait WithSourceId extends Buff {

  /** Entity responsible for putting the buff on its bearer. */
  def sourceId: Entity.Id

  final def maybeSourceId: Option[Entity.Id] = Some(sourceId)

}
