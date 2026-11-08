package gamelogic.gamestate.gameactions.spiritofnara

import gamelogic.buffs.Buff
import gamelogic.buffs.boss.spiritofnara.{MassSlow, Pulled}
import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithBuff, WithEntity}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import services.i18n.I18n

/** The Magnetic Pull: puts the player at `position` (next to the Spirit of Nara), where it stays
  * for a moment (see [[Pulled]]), and slows it down (see [[MassSlow]]).
  */
final case class PullPlayer(
    id: GameAction.Id,
    time: Long,
    bearerId: Entity.Id,
    position: Complex,
    pulledBuffId: Buff.Id,
    massSlowBuffId: Buff.Id
) extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    gameState
      .movingBodyEntityById(bearerId)
      .fold(GameStateTransformer.identityTransformer)(body =>
        WithEntity(
          body.move(time, position, body.direction, body.rotation, body.speed, moving = false),
          time
        ) ++
          WithBuff(Pulled(pulledBuffId, bearerId, time, position)) ++
          WithBuff(MassSlow(massSlowBuffId, bearerId, time))
      )

  def isLegal(gameState: GameState)(using I18n): None.type = None

  def changeId(newId: Id): PullPlayer = copy(id = newId)
}
