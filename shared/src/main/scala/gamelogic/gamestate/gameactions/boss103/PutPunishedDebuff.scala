package gamelogic.gamestate.gameactions.boss103

import gamelogic.buffs.Buff
import gamelogic.buffs.boss.boss103.Punished
import gamelogic.entities.Entity
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithBuff}
import gamelogic.physics.Complex

/** Puts an instance of [[Punished]] debuff on the target. */
final case class PutPunishedDebuff(
    id: GameAction.Id,
    time: Long,
    buffId: Buff.Id,
    bearerId: Entity.Id,
    sourceId: Entity.Id
) extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    new WithBuff(
      Punished(
        buffId,
        bearerId       = bearerId,
        appearanceTime = time,
        startingPos    = gameState.movingBodyEntityById(bearerId).fold(Complex.zero)(_.pos)
      )
    )

  def isLegal(gameState: GameState): Boolean = gameState.movingBodyEntityById(bearerId).isDefined

  def changeId(newId: Id): GameAction = copy(id = newId)
}
