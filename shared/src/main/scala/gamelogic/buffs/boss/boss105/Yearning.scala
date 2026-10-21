package gamelogic.buffs.boss.boss105

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, PassiveBuff}
import gamelogic.entities.Entity
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** Carried by the Shade when the boss calls it with [[gamelogic.abilities.boss.boss105.Reunion]].
  * It does not change anything by itself: it is the AI of the Shade that, while it is there, rushes
  * towards the boss.
  *
  * @param sourceId
  *   the boss
  */
final case class Yearning(
    buffId: Buff.Id,
    bearerId: Entity.Id,
    sourceId: Entity.Id,
    appearanceTime: Long
) extends PassiveBuff {
  def actionTransformer(gameAction: GameAction): Vector[GameAction] = Vector(gameAction)

  def duration: Long = Yearning.duration

  def resourceIdentifier: ResourceIdentifier = Buff.boss105Yearning

  def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Entity.Id])(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector.empty
}

object Yearning {

  /** How long the Shade rushes towards the boss. */
  val duration: Long = 6000L

  /** The speed of the Shade, relative to its normal one, when it answers the call. */
  inline def speedMultiplier: Double = 2.0
}
