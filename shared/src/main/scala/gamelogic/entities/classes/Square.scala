package gamelogic.entities.classes

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.AbilityId
import gamelogic.entities.Resource.{Rage, ResourceAmount}
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.{Entity, LivingEntity, MovingBody, WithAbilities}
import gamelogic.physics.Complex
import gamelogic.physics.shape.{Polygon, Shape}

/**
  * The [[gamelogic.entities.classes.Square]] is the tank class available to players.
  */
final case class Square(
    id: Entity.Id,
    time: Long,
    pos: Complex,
    direction: Angle,
    moving: Boolean,
    rotation: Angle,
    life: Double,
    colour: Int,
    relevantUsedAbilities: Map[AbilityId, Ability],
    maxLife: Double,
    speed: Double,
    resourceAmount: ResourceAmount
) extends PlayerClass {
  protected def patchLifeTotal(newLife: Double): LivingEntity = copy(life = newLife)

  def abilities: Set[AbilityId] = Set(Ability.squareTauntId)

  def useAbility(ability: Ability): WithAbilities = copy(
    relevantUsedAbilities = relevantUsedAbilities + (ability.abilityId -> ability),
    resourceAmount        = resourceAmount - ability.cost
  )

  def shape: Polygon = Shape.regularPolygon(4, Constants.playerRadius)

  def move(time: Long, position: Complex, direction: Angle, rotation: Angle, speed: Double, moving: Boolean): Square =
    copy(time = time, pos = position, direction = direction, rotation = rotation, speed = speed, moving = moving)
}

object Square {

  def initialResourceAmount: ResourceAmount = ResourceAmount(100, Rage)

}
