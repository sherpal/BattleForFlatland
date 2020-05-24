package gamelogic.entities.classes

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.AbilityId
import gamelogic.entities.Resource.{Rage, ResourceAmount}
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.{Entity, LivingEntity, WithAbilities}
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.gameactions.PutBasicShield
import gamelogic.physics.Complex
import gamelogic.physics.shape.{Polygon, Shape}
import gamelogic.utils.IdGeneratorContainer

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
    resourceAmount: ResourceAmount,
    maxResourceAmount: Double,
    name: String
) extends PlayerClass {
  protected def patchLifeTotal(newLife: Double): LivingEntity = copy(life = newLife)

  def abilities: Set[AbilityId] = Square.abilities

  def useAbility(ability: Ability): WithAbilities = copy(
    relevantUsedAbilities = relevantUsedAbilities + (ability.abilityId -> ability),
    resourceAmount        = resourceAmount - ability.cost
  )

  def shape: Polygon = Shape.regularPolygon(4, Constants.playerRadius)

  def move(time: Long, position: Complex, direction: Angle, rotation: Angle, speed: Double, moving: Boolean): Square =
    copy(time = time, pos = position, direction = direction, rotation = rotation, speed = speed, moving = moving)

  def teamId: Entity.TeamId = Entity.teams.playerTeam
}

object Square extends PlayerClassBuilder {

  def initialResourceAmount: ResourceAmount = ResourceAmount(100, Rage)

  final val abilities = Set(Ability.squareTauntId, Ability.squareHammerHit)

  final val initialMaxLife: Double = 200

  def startingActions(time: Long, entityId: Entity.Id, idGeneratorContainer: IdGeneratorContainer): List[GameAction] =
    List(PutBasicShield(0L, time, idGeneratorContainer.buffIdGenerator(), entityId))
}
