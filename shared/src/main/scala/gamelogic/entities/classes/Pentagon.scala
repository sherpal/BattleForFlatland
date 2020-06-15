package gamelogic.entities.classes

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.AbilityId
import gamelogic.buffs.Buff
import gamelogic.entities.Entity.{Id, TeamId}
import gamelogic.entities.{Entity, LivingEntity, MovingBody, Resource, WithAbilities}
import gamelogic.entities.Resource.ResourceAmount
import gamelogic.entities.WithPosition.Angle
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.gameactions.PutSimpleBuff
import gamelogic.physics.Complex
import gamelogic.physics.shape.{Polygon, Shape}
import gamelogic.utils.IdGeneratorContainer

final case class Pentagon(
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
  def shape: Polygon = Pentagon.shape

  protected def patchLifeTotal(newLife: Double): Pentagon = copy(life = newLife)

  def abilities: Set[AbilityId] = Pentagon.abilities

  def useAbility(ability: Ability): Pentagon = copy(
    relevantUsedAbilities = relevantUsedAbilities + (ability.abilityId -> ability),
    resourceAmount        = resourceAmount - ability.cost
  )

  protected def patchResourceAmount(newResourceAmount: ResourceAmount): Pentagon =
    copy(resourceAmount = newResourceAmount)

  def move(time: Long, position: Complex, direction: Angle, rotation: Angle, speed: Double, moving: Boolean): Pentagon =
    copy(time = time, pos = position, direction = direction, rotation = rotation, speed = speed, moving = moving)

  def teamId: TeamId = Entity.teams.playerTeam
}

object Pentagon extends PlayerClassBuilder {
  def startingActions(time: Long, entityId: Id, idGeneratorContainer: IdGeneratorContainer): List[GameAction] = List(
    PutSimpleBuff(
      idGeneratorContainer.gameActionIdGenerator(),
      time,
      idGeneratorContainer.buffIdGenerator(),
      entityId,
      time,
      Buff.manaFiller
    )
  )

  final val abilities: Set[Ability.AbilityId] = Set(Ability.pentagonPentagonBullet, Ability.createPentagonZoneId)

  final val shape: Polygon = Shape.regularPolygon(5, Constants.playerRadius)

  final val initialMaxLife: Double                = 100
  final val initialResourceAmount: ResourceAmount = ResourceAmount(300.0, Resource.Mana)
}
