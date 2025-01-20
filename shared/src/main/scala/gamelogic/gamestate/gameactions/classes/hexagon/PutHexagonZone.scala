package gamelogic.gamestate.gameactions.classes.hexagon

import gamelogic.gamestate.GameAction
import gamelogic.entities.Entity
import gamelogic.physics.Complex
import gamelogic.entities.WithPosition.Angle
import utils.misc.RGBAColour
import gamelogic.buffs.Buff
import gamelogic.gamestate.GameState
import gamelogic.gamestate.statetransformers.GameStateTransformer
import gamelogic.entities.classes.hexagon.HexagonZone
import gamelogic.gamestate.statetransformers.WithEntity
import gamelogic.gamestate.statetransformers.WithBuff
import gamelogic.gamestate.GameAction.Id

final case class PutHexagonZone(
    id: GameAction.Id,
    time: Long,
    zoneId: Entity.Id,
    position: Complex,
    rotation: Angle,
    heal: Double,
    sourceId: Entity.Id,
    colour: RGBAColour,
    buffId: Buff.Id
) extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer = {
    val zone = HexagonZone(zoneId, time, position, rotation, heal, sourceId, colour)
    val buff = zone.itsBuff(buffId)
    WithEntity(zone, time) ++ WithBuff(buff)
  }

  def isLegal(gameState: GameState): Option[String] = None

  def changeId(newId: Id): GameAction = copy(id = newId)
}
