package gamelogic.entities

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.AbilityId
import gamelogic.entities.Resource.{NoResource, ResourceAmount}
import gamelogic.entities.WithPosition.Angle
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
  def abilities: Set[AbilityId] = Set(
    Ability.simpleBulletId
  )

  def useAbility(ability: Ability): DummyLivingEntity = copy(
    relevantUsedAbilities = relevantUsedAbilities + (ability.abilityId -> ability)
  )

  val rotation: Double = 0.0

  val maxLife: Double = 100.0

  def resourceAmount: ResourceAmount = ResourceAmount(0.0, NoResource)
  def maxResourceAmount: Double      = 0.0

  protected def patchLifeTotal(newLife: Double): DummyLivingEntity = copy(life = newLife)

  def move(
      time: Long,
      position: Complex,
      direction: Angle,
      rotation: Angle,
      speed: Double,
      moving: Boolean
  ): DummyLivingEntity =
    copy(time = time, pos = position, direction = direction, moving = moving)

  def teamId: Entity.TeamId = Entity.teams.playerTeam

  protected def patchResourceAmount(newResourceAmount: ResourceAmount): WithAbilities = this
}

object DummyLivingEntity {

  /** Game distance unit per second. See [[gamelogic.entities.Moving]] for its usage. */
  final val speed = 200.0

  final val shape: Shape = new Circle(10.0)

}
