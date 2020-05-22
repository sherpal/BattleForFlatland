package gamelogic.entities.classes

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.AbilityId
import gamelogic.entities.Resource.{Mana, ResourceAmount}
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.{Entity, LivingEntity}
import gamelogic.physics.Complex
import gamelogic.physics.shape.{Polygon, Shape}

/**
  * The [[gamelogic.entities.classes.Hexagon]] is the healer class available to players.
  *
  * Obviously, it is represented as a Hexagon in the game.
  */
final case class Hexagon(
    id: Long,
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
    resourceAmount: ResourceAmount,
    maxResourceAmount: Double,
    name: String
) extends PlayerClass {

  protected def patchLifeTotal(newLife: Double): LivingEntity = copy(life = newLife)

  def abilities: Set[AbilityId] = Set(Ability.hexagonFlashHealId, Ability.hexagonHexagonHotId, Ability.simpleBulletId)

  def useAbility(ability: Ability): Hexagon = copy(
    relevantUsedAbilities = relevantUsedAbilities + (ability.abilityId -> ability),
    resourceAmount        = resourceAmount - ability.cost
  )

  def shape: Polygon = Shape.regularPolygon(6, Constants.playerRadius)

  def move(
      time: Long,
      position: Complex,
      direction: Angle,
      rotation: Angle,
      speed: Double,
      moving: Boolean
  ): Hexagon =
    copy(time = time, pos = position, direction = direction, rotation = rotation, speed = speed, moving = moving)

  def teamId: Entity.TeamId = Entity.teams.playerTeam
}

object Hexagon {
  def initialResourceAmount: ResourceAmount = ResourceAmount(300, Mana)
}
