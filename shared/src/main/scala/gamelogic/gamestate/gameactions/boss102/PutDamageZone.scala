package gamelogic.gamestate.gameactions.boss102

import gamelogic.buffs.Buff
import gamelogic.entities.Entity
import gamelogic.entities.Entity.TeamId
import gamelogic.entities.boss.boss102.DamageZone
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithBuff, WithEntity}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex

final case class PutDamageZone(
    id: GameAction.Id,
    time: Long,
    zoneId: Entity.Id,
    position: Complex,
    radius: Double,
    sourceId: Entity.Id,
    teamId: TeamId,
    buffId: Buff.Id
) extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer = {
    val zone = DamageZone(zoneId, time, position, radius, sourceId, teamId)

    new WithEntity(zone, time) ++ new WithBuff(zone.buff(buffId))
  }

  def isLegal(gameState: GameState): None.type = None

  def changeId(newId: Id): GameAction = copy(id = newId)
}
