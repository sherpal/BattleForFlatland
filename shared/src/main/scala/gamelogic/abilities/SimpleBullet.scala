package gamelogic.abilities

import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.NewSimpleBullet
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.utils.EntityIdGenerator

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

  def createActions(gameState: GameState, entityIdGenerator: EntityIdGenerator): List[GameAction] = List(
    NewSimpleBullet(
      0L,
      time,
      entityIdGenerator(),
      startingPosition,
      SimpleBullet.speed,
      direction,
      casterId,
      SimpleBullet.range
    )
  )

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability =
    copy(useId = newId, time = newTime)
}

object SimpleBullet {

  /** Game unit per second */
  final def speed: Double = 100

  /** Game unit */
  final def range: Double = speed * 5

}
