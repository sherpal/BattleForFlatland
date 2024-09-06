package gamelogic.abilities

import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.entities.Entity
import gamelogic.entities.Resource.{NoResource, ResourceAmount}
import gamelogic.gamestate.gameactions.NewSimpleBullet
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.utils.IdGeneratorContainer

final case class SimpleBullet(
    useId: Ability.UseId,
    time: Long,
    casterId: Entity.Id,
    startingPosition: Complex,
    direction: Double
) extends Ability {
  val abilityId: AbilityId = Ability.simpleBulletId
  val cooldown: Long       = 0L
  val castingTime: Long    = 1500L

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] = Vector(
    NewSimpleBullet(
      genActionId(),
      time,
      genEntityId(),
      startingPosition,
      SimpleBullet.speed,
      direction,
      casterId,
      SimpleBullet.range
    )
  )

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): SimpleBullet =
    copy(useId = newId, time = newTime)

  val cost: ResourceAmount = ResourceAmount(0.0, NoResource)

  def canBeCast(gameState: GameState, time: Long): None.type = None
}

object SimpleBullet {

  /** Game unit per second */
  final def speed: Double = 100

  /** Game unit */
  final def range: Double = speed * 5

}
