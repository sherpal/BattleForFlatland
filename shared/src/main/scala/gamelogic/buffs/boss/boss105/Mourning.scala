package gamelogic.buffs.boss.boss105

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, PassiveBuff}
import gamelogic.entities.Entity
import gamelogic.entities.boss.boss105.Shade
import gamelogic.gamestate.gameactions.RemoveEntity
import gamelogic.gamestate.gameactions.boss105.AddShade
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.utils.IdGeneratorContainer

/** Carried by Boss105 from the moment its Shade falls to the moment the Shade comes back: the boss
  * is exposed (it takes the full damage) and frenzied (it hits harder and faster).
  *
  * When it ends, the Shade comes back at `position`, where a
  * [[gamelogic.entities.boss.boss105.ReformationPoint]] announced it.
  *
  * @param markerId
  *   the reformation point to remove when the Shade is back
  */
final case class Mourning(
    buffId: Buff.Id,
    bearerId: Entity.Id,
    appearanceTime: Long,
    markerId: Entity.Id,
    position: Complex
) extends PassiveBuff {
  def actionTransformer(gameAction: GameAction): Vector[GameAction] = Vector(gameAction)

  def duration: Long = Mourning.duration

  def resourceIdentifier: ResourceIdentifier = Buff.boss105Mourning

  def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Entity.Id])(using
      IdGeneratorContainer
  ): Vector[GameAction] =
    RemoveEntity(genActionId(), time, markerId) +:
      Option
        .when(gameState.bosses.contains(bearerId) && gameState.allTEntities[Shade].isEmpty)(
          AddShade(
            genActionId(),
            time,
            genEntityId(),
            position,
            bearerId,
            genBuffId(),
            genBuffId(),
            genBuffId()
          )
        )
        .toVector

  /** If the boss dies, there is nothing to come back for. */
  override def bearerDiedAction(gameState: GameState, time: Long)(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector(RemoveEntity(genActionId(), time, markerId))
}

object Mourning {

  /** How long the boss is exposed: the delay before the Shade is back. */
  val duration: Long = 22000L
}
