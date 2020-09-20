package gamelogic.gamestate.gameactions.boss103

import cats.kernel.Monoid
import gamelogic.buffs.Buff
import gamelogic.buffs.boss.boss103.Purified
import gamelogic.entities.Entity
import gamelogic.gamestate.statetransformers.{GameStateTransformer, RemoveBuffTransformer, WithBuff}
import gamelogic.gamestate.{GameAction, GameState}

/** Puts an instance of [[Purified]] buff on the target. */
final case class PutPurifiedDebuff(
    id: GameAction.Id,
    time: Long,
    buffId: Buff.Id,
    bearerId: Entity.Id,
    sourceId: Entity.Id
) extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    Monoid[GameStateTransformer].combineAll(
      gameState.passiveBuffs
        .get(bearerId)
        .fold(List[GameStateTransformer]())(
          _.valuesIterator
            .collect { case buff: Purified if buff.buffId != buffId => buff }
            .map(
              buff => new RemoveBuffTransformer(time, bearerId, buff.buffId)
            )
            .toList
        )
    ) ++ new WithBuff(
      Purified(
        buffId         = buffId,
        bearerId       = bearerId,
        sourceId       = sourceId,
        appearanceTime = time
      )
    )

  def isLegal(gameState: GameState): Boolean = true

  def changeId(newId: GameAction.Id): PutPurifiedDebuff = copy(id = newId)
}
