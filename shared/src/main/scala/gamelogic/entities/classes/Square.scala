package gamelogic.entities.classes

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.AbilityId
import gamelogic.buffs.Buff
import gamelogic.entities.Resource.{Rage, ResourceAmount}
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.{Entity, LivingEntity}
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.gameactions.PutSimpleBuff
import gamelogic.physics.Complex
import gamelogic.physics.shape.{Polygon, Shape}
import gamelogic.utils.IdGeneratorContainer

/** The [[gamelogic.entities.classes.Square]] is the tank class available to players.
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

  def useAbility(ability: Ability): Square = copy(
    relevantUsedAbilities = relevantUsedAbilities + (ability.abilityId -> ability),
    resourceAmount = resourceAmount - ability.cost
  )

  def shape: Polygon = Square.shape

  def move(
      time: Long,
      position: Complex,
      direction: Angle,
      rotation: Angle,
      speed: Double,
      moving: Boolean
  ): Square =
    copy(
      time = time,
      pos = position,
      direction = direction,
      rotation = rotation,
      speed = speed,
      moving = moving
    )

  def teamId: Entity.TeamId = Entity.teams.playerTeam

  protected def patchResourceAmount(newResourceAmount: ResourceAmount): Square =
    copy(resourceAmount = newResourceAmount)
}

object Square extends PlayerClassBuilder {

  final val shape = Shape.regularPolygon(4, Constants.playerRadius)

  def initialResourceAmount: ResourceAmount = ResourceAmount(100, Rage)

  final val abilities =
    Set(
      Ability.squareTauntId,
      Ability.squareHammerHit,
      Ability.squareEnrageId,
      Ability.squareCleaveId
    )

  final val initialMaxLife: Double = 200

  def startingActions(
      time: Long,
      entityId: Entity.Id
  )(using IdGeneratorContainer): Vector[GameAction] =
    Vector(
      PutSimpleBuff(
        GameAction.Id.zero,
        time,
        genBuffId(),
        entityId,
        entityId,
        time,
        Buff.rageFiller
      ),
      PutSimpleBuff(
        GameAction.Id.zero,
        time,
        genBuffId(),
        entityId,
        entityId,
        time,
        Buff.squareDefaultShield
      )
    )
}
