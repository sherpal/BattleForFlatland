package gamelogic.buffs.abilities.classes

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, PassiveBuff}
import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.GameState
import gamelogic.gamestate.gameactions.{EntityTakesDamage, RemoveBuff}
import gamelogic.utils.IdGeneratorContainer

/** The buff that the caster of [[gamelogic.abilities.square.Guard]] has: a share of the damage that
  * `guardedId` takes is taken by the bearer instead.
  *
  * The buff is on the guardian, and not on the guarded ally, so that it is gone when the guardian
  * dies (and the damage is not lost). The ally has a [[Guarded]] buff (`guardedBuffId`), which only
  * shows that it is protected, and which goes away with this one.
  */
final case class Guarding(
    buffId: Buff.Id,
    bearerId: Entity.Id,
    sourceId: Entity.Id,
    appearanceTime: Long,
    guardedId: Entity.Id,
    guardedBuffId: Buff.Id
) extends PassiveBuff {

  def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Entity.Id])(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector.empty

  def actionTransformer(gameAction: GameAction): Vector[GameAction] = gameAction match {
    case damage: EntityTakesDamage if damage.entityId == guardedId && guardedId != bearerId =>
      val redirected = damage.amount * Guarding.redirectedShare
      Vector(
        damage.copy(amount = damage.amount - redirected),
        damage.copy(entityId = bearerId, amount = redirected)
      )
    case _ => Vector(gameAction)
  }

  /** The ally is not protected anymore. */
  override def bearerDiedAction(gameState: GameState, time: Long)(using
      IdGeneratorContainer
  ): Vector[GameAction] =
    Vector(RemoveBuff(genActionId(), time, guardedId, guardedBuffId))

  /** The damage is shared before anything else reduces it, and what the guardian takes goes through
    * its own buffs (its shield, and the rage it gets from it) like any other damage.
    */
  override def transformerOrder: Int = Guarding.transformerOrder

  def duration: Long = Guarding.duration

  def resourceIdentifier: ResourceIdentifier = Buff.squareGuarding
}

object Guarding {

  /** Share of the damage taken by the guarded ally that goes to the guardian. */
  inline def redirectedShare: Double = 0.4

  /** Time (in millis) during which the guardian protects its ally. */
  inline def duration: Long = 8000L

  /** Before the [[gamelogic.buffs.resourcebuffs.RageFiller]], which is at -1. */
  inline def transformerOrder: Int = -2

}
