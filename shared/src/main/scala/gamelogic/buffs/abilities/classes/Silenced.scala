package gamelogic.buffs.abilities.classes

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{ActionPreventerBuff, Buff, PassiveBuff, SilencingBuff}
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.UseAbility
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

case class Silenced(
    buffId: Buff.Id,
    bearerId: Entity.Id,
    sourceId: Entity.Id,
    appearanceTime: Long
) extends PassiveBuff
    with SilencingBuff {
  def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Entity.Id])(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector.empty

  override def actionTransformer(gameAction: GameAction): Vector[GameAction] =
    if isActionPrevented(gameAction) then Vector.empty else Vector(gameAction)

  override def duration: Long = Silenced.duration

  override def resourceIdentifier: ResourceIdentifier = Buff.silence
}

object Silenced {
  val duration: Long = 7000L
}
