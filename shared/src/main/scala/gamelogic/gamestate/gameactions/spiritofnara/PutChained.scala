package gamelogic.gamestate.gameactions.spiritofnara

import gamelogic.buffs.Buff
import gamelogic.buffs.boss.spiritofnara.Chained
import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithBuff, WithEntity}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import services.i18n.I18n

/** Chains the player where it is (see [[Chained]]). */
final case class PutChained(
    id: GameAction.Id,
    time: Long,
    buffId: Buff.Id,
    bearerId: Entity.Id,
    position: Complex
) extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    gameState
      .movingBodyEntityById(bearerId)
      .fold(GameStateTransformer.identityTransformer)(body =>
        WithEntity(
          body.move(time, position, body.direction, body.rotation, body.speed, moving = false),
          time
        ) ++ WithBuff(Chained(buffId, bearerId, time, position))
      )

  def isLegal(gameState: GameState)(using I18n): None.type = None

  def changeId(newId: Id): PutChained = copy(id = newId)
}
