package gamelogic.buffs.boss.boss105

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, PassiveBuff}
import gamelogic.entities.Entity
import gamelogic.entities.boss.boss105.Shade
import gamelogic.entities.boss.dawnoftime.Boss105
import gamelogic.gamestate.gameactions.RemoveBuff
import gamelogic.gamestate.gameactions.boss105.ShadeFalls
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** Invisible, never-ending buff carried by the [[Shade]], whose sole purpose is to tell the boss
  * when its bearer dies: the boss loses its [[TwinBond]] and starts [[Mourning]].
  *
  * @param sourceId
  *   the boss
  */
final case class ShadeBond(
    buffId: Buff.Id,
    bearerId: Entity.Id,
    sourceId: Entity.Id,
    appearanceTime: Long
) extends PassiveBuff {
  def actionTransformer(gameAction: GameAction): Vector[GameAction] = Vector(gameAction)

  def duration: Long = -1L

  def resourceIdentifier: ResourceIdentifier = Buff.boss105ShadeBond

  def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Entity.Id])(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector.empty

  /** This is called on a game state where the Shade is still there. The Shade comes back in the
    * corner of the room which is the farthest from where the boss is now.
    */
  override def bearerDiedAction(gameState: GameState, time: Long)(using
      IdGeneratorContainer
  ): Vector[GameAction] =
    gameState.bosses.get(sourceId).toVector.flatMap { boss =>
      val unbond = gameState
        .allBuffsOfEntity(sourceId)
        .collect { case bond: TwinBond => RemoveBuff(genActionId(), time, sourceId, bond.buffId) }
        .toVector

      val position = Shade.reformationPosition(Boss105.size, boss.currentPosition(time))

      unbond :+ ShadeFalls(genActionId(), time, sourceId, genBuffId(), genEntityId(), position)
    }
}
