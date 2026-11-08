package gamelogic.gamestate.gameactions.spiritofnara

import gamelogic.buffs.Buff
import gamelogic.buffs.boss.spiritofnara.BlizzardTicks
import gamelogic.entities.Entity
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.boss.spiritofnara.BlizzardCloud
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithBuff, WithEntity}
import gamelogic.gamestate.{GameAction, GameState}
import services.i18n.I18n

/** Puts the [[BlizzardCloud]] in the room, and the [[BlizzardTicks]] on the boss. */
final case class AddBlizzard(
    id: GameAction.Id,
    time: Long,
    entityId: Entity.Id,
    startAngle: Angle,
    buffId: Buff.Id,
    bossId: Entity.Id
) extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    WithEntity(BlizzardCloud(entityId, time, startAngle), time) ++
      WithBuff(BlizzardTicks(buffId, bossId, time, time, entityId))

  def isLegal(gameState: GameState)(using I18n): None.type = None

  def changeId(newId: Id): AddBlizzard = copy(id = newId)
}
