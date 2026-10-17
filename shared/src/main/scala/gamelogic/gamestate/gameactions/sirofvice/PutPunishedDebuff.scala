package gamelogic.gamestate.gameactions.sirofvice

import gamelogic.buffs.Buff
import gamelogic.buffs.boss.sirofvice.Punished
import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithBuff, WithEntity}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex

/** Puts an instance of [[Punished]] debuff on the target, and stops it where it is.
  *
  * @param position
  *   where the bearer is when the debuff is applied (and where it will stay)
  */
final case class PutPunishedDebuff(
    id: GameAction.Id,
    time: Long,
    buffId: Buff.Id,
    bearerId: Entity.Id,
    sourceId: Entity.Id,
    position: Complex
) extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    gameState
      .movingBodyEntityById(bearerId)
      .fold(GameStateTransformer.identityTransformer)(body =>
        WithEntity(
          body.move(time, position, body.direction, body.rotation, body.speed, moving = false),
          time
        )
      ) ++ WithBuff(
      Punished(buffId, bearerId = bearerId, startingPos = position, appearanceTime = time)
    )

  def isLegal(gameState: GameState): Option[String] =
    gameState.movingBodyEntityById(bearerId) match {
      case None    => Some(s"Entity $bearerId does not exist, or is not a Moving Body")
      case Some(_) => None
    }

  def changeId(newId: Id): GameAction = copy(id = newId)
}
