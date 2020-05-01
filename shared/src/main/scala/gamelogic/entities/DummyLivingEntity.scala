package gamelogic.entities

import gamelogic.physics.Complex
import gamelogic.physics.shape.{Circle, Shape}

/**
  * Represents a small living entity with a given position and direction towards which this entity is heading.
  */
final case class DummyLivingEntity(
    id: Long,
    pos: Complex,
    direction: Double,
    moving: Boolean,
    life: Double,
    colour: Int
) extends LivingEntity
    with Moving {

  val speed: Double = DummyLivingEntity.speed

  val shape: Shape = DummyLivingEntity.shape

}

object DummyLivingEntity {

  /** Game distance unit per second. See [[gamelogic.entities.Moving]] for its usage. */
  final val speed = 200.0

  final val shape: Shape = new Circle(5.0)

}
