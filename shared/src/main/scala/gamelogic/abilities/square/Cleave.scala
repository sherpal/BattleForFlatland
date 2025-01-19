package gamelogic.abilities.square

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.classes.Constants
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.physics.shape.{ConvexPolygon, Polygon}
import gamelogic.utils.IdGeneratorContainer

final case class Cleave(
    useId: Ability.UseId,
    time: Long,
    casterId: Entity.Id,
    position: Complex,
    rotation: Angle
) extends Ability {
  def abilityId: AbilityId = Ability.squareCleaveId

  def cooldown: Long = Cleave.cooldown

  def castingTime: Long = 0L

  def cost: Resource.ResourceAmount = Cleave.cost

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    gameState.allLivingEntities
      .filter(_.teamId != Entity.teams.playerTeam)
      .filter(_.collidesShape(Cleave.cone, position, rotation, time))
      .map { entity =>
        EntityTakesDamage(genActionId(), time, entity.id, Cleave.damage, casterId)
      }
      .toVector

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): None.type = None
}

object Cleave {

  import Complex._

  @inline final def cooldown: Long                = 5000L
  @inline final def cost: Resource.ResourceAmount = Resource.ResourceAmount(50.0, Resource.Rage)
  @inline final def damage: Double                = 30.0
  @inline final def coneHeight: Double            = 3 * Constants.playerRadius
  @inline final def coneSpread: Double            = math.Pi / 3

  val cone: Polygon = new ConvexPolygon(
    Vector(
      Complex.zero,
      coneHeight - coneHeight.i * math.tan(coneSpread / 2),
      coneHeight + coneHeight.i * math.tan(coneSpread / 2)
    )
  )

}
