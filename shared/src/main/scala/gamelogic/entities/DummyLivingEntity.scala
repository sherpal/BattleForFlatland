package gamelogic.entities

import gamelogic.physics.Complex

/**
  * Represents a small living entity with a given position and direction towards which this entity is heading.
  */
final case class DummyLivingEntity(
    id: Long,
    pos: Complex,
    direction: Double,
    moving: Boolean,
    life: Double
) extends LivingEntity
    with Moving {

  val speed: Double = DummyLivingEntity.speed

}

object DummyLivingEntity {

  /** Game distance unit per second. See [[gamelogic.entities.Moving]] for its usage. */
  final val speed = 200.0

}
