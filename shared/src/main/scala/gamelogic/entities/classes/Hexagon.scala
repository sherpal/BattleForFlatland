package gamelogic.entities.classes

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.AbilityId
import gamelogic.entities.Resource.ResourceAmount
import gamelogic.entities.{LivingEntity, MovingBody, WithAbilities}
import gamelogic.physics.Complex
import gamelogic.physics.shape.Shape

/**
  * The [[gamelogic.entities.classes.Hexagon]] is the healer class available to players.
  *
  * Obviously, it is represented as a Hexagon in the game.
  */
final case class Hexagon(
    id: Long,
    time: Long,
    pos: Complex,
    direction: Double,
    moving: Boolean,
    rotation: Double,
    life: Double,
    colour: Int,
    relevantUsedAbilities: Map[AbilityId, Ability],
    maxLife: Double,
    speed: Double,
    resourceAmount: ResourceAmount
) extends LivingEntity
    with MovingBody
    with WithAbilities {

  protected def patchLifeTotal(newLife: Double): LivingEntity = copy(life = newLife)

  def abilities: Set[AbilityId] = Set(Ability.hexagonFlashHealId, Ability.hexagonHexagonHotId)

  def useAbility(ability: Ability): Hexagon = copy(
    relevantUsedAbilities = relevantUsedAbilities + (ability.abilityId -> ability),
    resourceAmount        = resourceAmount - ability.cost
  )

  def shape: Shape = Shape.regularPolygon(6, Constants.playerRadius)
}
