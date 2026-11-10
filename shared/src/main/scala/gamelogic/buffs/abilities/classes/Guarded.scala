package gamelogic.buffs.abilities.classes

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, PassiveBuff, WithSourceId}
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.RemoveBuff
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** What the ally protected by a [[gamelogic.abilities.square.Guard]] has, to show it: it does
  * nothing by itself, since it is the [[Guarding]] of its guardian (`sourceId`) that takes part of
  * the damage.
  */
final case class Guarded(
    buffId: Buff.Id,
    bearerId: Entity.Id,
    sourceId: Entity.Id,
    appearanceTime: Long,
    guardingBuffId: Buff.Id
) extends PassiveBuff
    with WithSourceId {

  def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Entity.Id])(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector.empty

  /** The guardian has nobody to protect anymore. */
  override def bearerDiedAction(gameState: GameState, time: Long)(using
      IdGeneratorContainer
  ): Vector[GameAction] =
    Vector(RemoveBuff(genActionId(), time, sourceId, guardingBuffId))

  def actionTransformer(gameAction: GameAction): Vector[GameAction] = Vector(gameAction)

  def duration: Long = Guarding.duration

  def resourceIdentifier: ResourceIdentifier = Buff.squareGuarded
}
