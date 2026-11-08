package gamelogic.buffs.boss.spiritofnara

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, PassiveBuff}
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.EntityStartsCasting
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** The bearer can't use abilities costing mana. */
final case class Counterspelled(buffId: Buff.Id, bearerId: Entity.Id, appearanceTime: Long)
    extends PassiveBuff {

  def actionTransformer(gameAction: GameAction): Vector[GameAction] = gameAction match {
    case EntityStartsCasting(_, _, _, ability)
        if ability.resource == Resource.Mana && ability.casterId == bearerId =>
      Vector.empty
    case action => Vector(action)
  }

  def duration: Long = Counterspelled.duration

  def resourceIdentifier: ResourceIdentifier = Buff.spiritOfNaraCounterspelled

  def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Entity.Id])(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector.empty
}

object Counterspelled {
  inline def duration: Long = 4000L
}
