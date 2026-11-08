package gamelogic.buffs.boss.spiritofnara

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, PassiveBuff}
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.MovingBodyMoves
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.utils.IdGeneratorContainer

/** Roots the bearer where it stands (it can still use its abilities). Can be dispelled. */
final case class Chained(
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

  def duration: Long = Chained.duration

  def resourceIdentifier: ResourceIdentifier = Buff.spiritOfNaraChained

  def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Entity.Id])(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector.empty

  override def canBeDispelled: Boolean = true
}

object Chained {
  inline def duration: Long = 6000L
}
