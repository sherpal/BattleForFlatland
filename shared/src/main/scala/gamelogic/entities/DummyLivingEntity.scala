package gamelogic.entities

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.AbilityId
import gamelogic.physics.Complex
import gamelogic.physics.shape.{Circle, Shape}

/**
  * Represents a small living entity with a given position and direction towards which this entity is heading.
  */
final case class DummyLivingEntity(
    id: Long,
    time: Long,
    pos: Complex,
    direction: Double,
    moving: Boolean,
    life: Double,
    colour: Int,
    relevantUsedAbilities: Map[AbilityId, Ability]
) extends LivingEntity
    with MovingBody
    with WithAbilities {

  val speed: Double = DummyLivingEntity.speed

  val shape: Shape = DummyLivingEntity.shape
  val abilities: Set[AbilityId] = Set(
    Ability.simpleBulletId
  )

  def useAbility(ability: Ability): WithAbilities = copy(
    relevantUsedAbilities = relevantUsedAbilities + (ability.abilityId -> ability)
  )

  val rotation: Double = 0.0
}

object DummyLivingEntity {

  /** Game distance unit per second. See [[gamelogic.entities.Moving]] for its usage. */
  final val speed = 200.0

  final val shape: Shape = new Circle(10.0)

}
