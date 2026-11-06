package gamelogic.buffs.abilities.classes

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, PassiveBuff}
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.classes.pentagon.PutFractured
import gamelogic.gamestate.gameactions.{EntityTakesDamage, RemoveBuff}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.{IdGeneratorContainer, IdsProducer}

/** Put by the [[gamelogic.abilities.pentagon.Fracture]] of the Pentagon `sourceId`: the bearer
  * takes `Fractured.damageIncrease` more damage from that Pentagon (and only from it).
  *
  * Its duration is not always the same: fracturing an enemy again before the end adds what was
  * left, up to `Fractured.maxCarriedOver` (see `Fractured.refresh`).
  */
final case class Fractured(
    buffId: Buff.Id,
    bearerId: Entity.Id,
    sourceId: Entity.Id,
    appearanceTime: Long,
    duration: Long
) extends PassiveBuff {

  def actionTransformer(gameAction: GameAction): Vector[GameAction] = Vector(gameAction match {
    case damage: EntityTakesDamage if damage.entityId == bearerId && damage.sourceId == sourceId =>
      damage.copy(amount = damage.amount * (1 + Fractured.damageIncrease))
    case _ => gameAction
  })

  def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Entity.Id])(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector.empty

  def resourceIdentifier: ResourceIdentifier = Buff.pentagonFractured
}

object Fractured {

  /** Share of damage that the bearer additionally takes from the Pentagon. */
  inline def damageIncrease: Double = 0.08

  inline def duration: Long = 12000L

  /** What is left of the previous one when fracturing again (in millis), at most. */
  inline def maxCarriedOver: Long = 4000L

  /** The [[Fractured]] that `sourceId` put on `bearerId`, if any. */
  def by(gameState: GameState, bearerId: Entity.Id, sourceId: Entity.Id): Option[Fractured] =
    gameState
      .allBuffsOfEntity(bearerId)
      .collectFirst { case fractured: Fractured if fractured.sourceId == sourceId => fractured }

  /** Actions fracturing `bearerId` at `time` for `sourceId`, replacing the previous one (if any),
    * whose remaining time is carried over (up to `maxCarriedOver`).
    */
  def refresh(gameState: GameState, bearerId: Entity.Id, sourceId: Entity.Id, time: Long)(using
      IdGeneratorContainer
  ): Vector[GameAction] = {
    val previous = by(gameState, bearerId, sourceId)
    val carriedOver = previous
      .fold(0L)(fractured => fractured.appearanceTime + fractured.duration - time)
      .max(0L)
      .min(maxCarriedOver)
    val removePrevious = previous.map(fractured =>
      RemoveBuff(IdsProducer.genActionId(), time, bearerId, fractured.buffId)
    )
    removePrevious.toVector :+ PutFractured(
      IdsProducer.genActionId(),
      time,
      IdsProducer.genBuffId(),
      bearerId,
      sourceId,
      duration + carriedOver
    )
  }

}
