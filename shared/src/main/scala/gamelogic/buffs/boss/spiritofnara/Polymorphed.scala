package gamelogic.buffs.boss.spiritofnara

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, PassiveBuff}
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.{
  EntityGetsHealed,
  EntityStartsCasting,
  MovingBodyMoves,
  UseAbility
}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.utils.IdGeneratorContainer

/** The bearer is turned into a harmless shape: it can neither move nor use abilities. When it wakes
  * up, it is back to full life.
  */
final case class Polymorphed(
    buffId: Buff.Id,
    bearerId: Entity.Id,
    sourceId: Entity.Id,
    appearanceTime: Long,
    position: Complex
) extends PassiveBuff {

  def actionTransformer(gameAction: GameAction): Vector[GameAction] = gameAction match {
    case action: MovingBodyMoves if action.entityId == bearerId =>
      Vector(action.copy(position = position, moving = false))
    case action: EntityStartsCasting if action.ability.casterId == bearerId => Vector.empty
    case action: UseAbility if action.casterId == bearerId => Vector.empty
    case action => Vector(action)
  }

  def duration: Long = Polymorphed.duration

  def resourceIdentifier: ResourceIdentifier = Buff.spiritOfNaraPolymorphed

  def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Entity.Id])(using
      IdGeneratorContainer
  ): Vector[GameAction] =
    gameState.players
      .get(bearerId)
      .map(player => EntityGetsHealed(genActionId(), time, bearerId, player.maxLife, sourceId))
      .toVector

  override def bearerDiedAction(gameState: GameState, time: Long)(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector.empty
}

object Polymorphed {
  inline def duration: Long = 8000L
}
