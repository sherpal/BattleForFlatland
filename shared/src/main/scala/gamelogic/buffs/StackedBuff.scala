package gamelogic.buffs

import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.PutStackedBuff
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.PosInt
import gamelogic.utils.IdGeneratorContainer

/** A buff that comes with a number of stacks. When it ends, it is replaced by the same buff with
  * one stack less, until there is no stack left.
  *
  * To make a stack go up, the source of the stacks should remove the current buff and put a new one
  * with more stacks, using [[gamelogic.gamestate.gameactions.PutStackedBuff]]. Its concrete type
  * must be registered in [[StackedBuffs]].
  */
trait StackedBuff extends WithSourceId {

  def resourceIdentifier: Buff.StackedBuffIdentifier

  /** Number of stacks of this buff. It is at least 1. */
  def stacks: PosInt

  /** The buff with one stack less takes over exactly when this one ends, even if the end is noticed
    * a bit later, so that the stacks don't drift.
    */
  def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Entity.Id])(using
      IdGeneratorContainer
  ): Vector[GameAction] =
    stacks
      .minus(1)
      .map { newStacks =>
        val endingTime = appearanceTime + duration
        PutStackedBuff(
          genActionId(),
          endingTime,
          genBuffId(),
          bearerId,
          sourceId,
          endingTime,
          resourceIdentifier,
          newStacks
        )
      }
      .toVector

  /** A dead bearer doesn't keep its stacks. */
  override def bearerDiedAction(gameState: GameState, time: Long)(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector.empty

}
