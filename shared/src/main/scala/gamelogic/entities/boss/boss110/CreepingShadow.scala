package gamelogic.entities.boss.boss110

import gamelogic.entities.MovingBody
import gamelogic.entities.WithPosition
import gamelogic.physics.Complex
import gamelogic.physics.Complex
import gamelogic.entities.Entity
import gamelogic.physics.shape.Shape
import gamelogic.physics.shape.Circle
import gamelogic.entities.WithPosition
import gamelogic.gamestate.GameState
import gamelogic.entities.boss.dawnoftime.Boss110
import gamelogic.entities.WithChangingRadius
import gamelogic.entities.classes.Constants
import gamelogic.entities.WithThreat
import gamelogic.entities.WithAbilities
import gamelogic.abilities.Ability
import gamelogic.entities.Resource

/**
  * Instance of the [[CreepingShadow]] in [[Boss110]].
  *
  * @param id id of the entity
  * @param time last update time
  * @param pos position at that time
  * @param radius current radius of the shadow (may --and will!-- change during the game)
  * @param sourceId entity id of the boss
  * @param direction where the shadow is moving to
  * @param moving whether its moving
  */
final case class CreepingShadow(
    id: Entity.Id,
    time: Long,
    pos: Complex,
    radius: Double,
    sourceId: Entity.Id,
    direction: WithPosition.Angle,
    moving: Boolean,
    relevantUsedAbilities: Map[Ability.AbilityId, Ability]
) extends MovingBody
    with WithChangingRadius
    with WithThreat
    with WithAbilities {

  def abilities: Set[Ability.AbilityId] = Set(Ability.boss110CreepingShadowTick)

  def useAbility(ability: Ability): CreepingShadow = copy(
    relevantUsedAbilities = relevantUsedAbilities + (ability.abilityId -> ability)
  )

  def resourceAmount: Resource.ResourceAmount = Resource.noResourceAmount

  def maxResourceAmount: Double = 0.0

  protected def patchResourceAmount(newResourceAmount: Resource.ResourceAmount): CreepingShadow = this

  def healingThreats: Map[Entity.Id, WithThreat.ThreatAmount] = Map.empty

  def damageThreats: Map[Entity.Id, WithThreat.ThreatAmount] = Map.empty

  def changeDamageThreats(threatId: Entity.Id, delta: WithThreat.ThreatAmount): CreepingShadow = this

  def changeHealingThreats(threatId: Entity.Id, delta: WithThreat.ThreatAmount): CreepingShadow = this

  def teamId: Entity.TeamId = Entity.teams.mobTeam

  def speed: Double = Constants.playerSpeed / 2

  def shape: Shape = new Circle(radius)

  def rotation: WithPosition.Angle = 0.0

  def move(
      time: Long,
      position: Complex,
      direction: WithPosition.Angle,
      rotation: WithPosition.Angle,
      speed: Double,
      moving: Boolean
  ): CreepingShadow =
    copy(time = time, pos = position, direction = direction, moving = moving)

  def changeRadius(newRadius: Double): CreepingShadow = copy(radius = newRadius)

}

object CreepingShadow {

  def extractCreepingShadow(gameState: GameState, entityId: Entity.Id): Option[CreepingShadow] =
    gameState.entityByIdAs[CreepingShadow](entityId)

  /**
    * Computes the current position and the current radius of the [[CreepingShadow]].
    *
    * The position is simply the center of mass of all [[SmallGuy]]s, while the radius is directly proportional to
    * the number of [[SmallGuy]]s, and is such that the whole game area is covered if there are at least
    * 10 [[SmallGuy]]s.
    */
  def computePositionAndRadius(gameState: GameState, currentTime: Long): (Complex, Double) = {
    val smallGuies = gameState.allTEntities[SmallGuy].values.toList

    val howManyGuies = smallGuies.length

    val radius = howManyGuies * (Boss110.halfWidth * 2) / 10.0
    val position =
      if (howManyGuies == 0) Complex(-Boss110.halfWidth, 0)
      else smallGuies.map(_.currentPosition(currentTime)).sum / howManyGuies

    (position, radius max 1)
  }

}
