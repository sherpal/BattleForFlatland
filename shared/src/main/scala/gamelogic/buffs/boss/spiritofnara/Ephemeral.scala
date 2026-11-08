package gamelogic.buffs.boss.spiritofnara

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, PassiveBuff}
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.RemoveEntity
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** The bearer (a Water Elemental) vanishes when this buff ends. */
final case class Ephemeral(buffId: Buff.Id, bearerId: Entity.Id, appearanceTime: Long)
    extends PassiveBuff {

  def actionTransformer(gameAction: GameAction): Vector[GameAction] = Vector(gameAction)

  def duration: Long = Ephemeral.duration

  def resourceIdentifier: ResourceIdentifier = Buff.spiritOfNaraEphemeral

  def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Entity.Id])(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector(RemoveEntity(genActionId(), time, bearerId))

  override def bearerDiedAction(gameState: GameState, time: Long)(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector.empty
}

object Ephemeral {
  inline def duration: Long = 60000L
}
