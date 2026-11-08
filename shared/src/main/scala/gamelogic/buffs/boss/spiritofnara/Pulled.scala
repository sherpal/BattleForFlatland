package gamelogic.buffs.boss.spiritofnara

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, PassiveBuff}
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.MovingBodyMoves
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.utils.IdGeneratorContainer

/** Keeps the bearer where the Magnetic Pull put it, for a short while.
  *
  * Without it, the next movement of a player (computed by its client from where it was before the
  * pull) would bring it back where it was.
  */
final case class Pulled(
    buffId: Buff.Id,
    bearerId: Entity.Id,
    appearanceTime: Long,
    position: Complex
) extends PassiveBuff {

  def actionTransformer(gameAction: GameAction): Vector[GameAction] = gameAction match {
    case action: MovingBodyMoves if action.entityId == bearerId =>
      Vector(action.copy(position = position, moving = false))
    case action => Vector(action)
  }

  def duration: Long = Pulled.duration

  def resourceIdentifier: ResourceIdentifier = Buff.spiritOfNaraPulled

  def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Entity.Id])(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector.empty
}

object Pulled {
  inline def duration: Long = 500L
}
