package gamelogic.entities.classes

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.AbilityId
import gamelogic.buffs.Buff
import gamelogic.entities.Entity.Id
import gamelogic.entities.Resource.{Energy, ResourceAmount}
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.gameactions.PutSimpleBuff
import gamelogic.physics.Complex
import gamelogic.physics.shape.{Polygon, Shape}
import gamelogic.utils.IdGeneratorContainer

/**
  * The [[Triangle]] is the melee dps class in BFF. It attacks close its enemies.
  *
  * The [[Triangle]] has an energy bar that is replenish very often (10 seconds to replenish it entirely), but most of
  * the attacks uses a lot amount of them.
  */
final case class Triangle(
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
    resourceAmount: ResourceAmount,
    maxResourceAmount: Double,
    name: String
) extends PlayerClass {

  def shape: Polygon = Triangle.shape

  def move(
      time: Long,
      position: Complex,
      direction: Angle,
      rotation: Angle,
      speed: Double,
      moving: Boolean
  ): Triangle = copy(
    time      = time,
    pos       = position,
    direction = direction,
    rotation  = rotation,
    speed     = speed,
    moving    = moving
  )

  def abilities: Set[AbilityId] = Triangle.abilities

  def useAbility(ability: Ability): Triangle = copy(
    relevantUsedAbilities = relevantUsedAbilities + (ability.abilityId -> ability),
    resourceAmount        = resourceAmount - ability.cost
  )

  protected def patchResourceAmount(newResourceAmount: Resource.ResourceAmount): Triangle =
    copy(resourceAmount = newResourceAmount)

  protected def patchLifeTotal(newLife: Double): Triangle = copy(life = newLife)

  def teamId: Entity.TeamId = Entity.teams.playerTeam
}

object Triangle extends PlayerClassBuilder {

  val shape = Shape.regularPolygon(3, Constants.playerRadius)

  def initialResourceAmount: ResourceAmount = ResourceAmount(100, Energy)

  val initialMaxLife: Double = 100

  val abilities: Set[Ability.AbilityId] = Set(
    Ability.triangleEnergyKick,
    Ability.triangleDirectHit,
    Ability.triangleUpgradeDirectHit,
    Ability.triangleStun
  )

  def startingActions(time: Id, entityId: Id, idGeneratorContainer: IdGeneratorContainer): List[GameAction] = List(
    PutSimpleBuff(0L, time, idGeneratorContainer.buffIdGenerator(), entityId, entityId, time, Buff.energyFiller)
  )
}
