package gamelogic.gamestate.gameactions.classes.pentagon

import gamelogic.buffs.Buff
import gamelogic.entities.Entity
import gamelogic.entities.classes.pentagon.PentagonZone
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithBuff, WithEntity}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import utils.misc.RGBAColour

/**
  * Adds a [[gamelogic.entities.classes.pentagon.PentagonZone]] to the game with the specified arguments.
  * Also adds its corresponding [[gamelogic.buffs.entities.classes.pentagon.PentagonZoneTick]] buff to actually
  * do stuff.
  */
final case class PutPentagonZone(
    id: GameAction.Id,
    time: Long,
    zoneId: Entity.Id,
    position: Complex,
    rotation: Double,
    damage: Double,
    sourceId: Entity.Id,
    colour: RGBAColour,
    buffId: Buff.Id
) extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer = {
    val zone = PentagonZone(zoneId, time, position, rotation, damage, sourceId, colour)
    val buff = zone.itsBuff(buffId)
    new WithEntity(zone, time) ++ new WithBuff(buff)
  }

  def isLegal(gameState: GameState): Boolean = true

  def changeId(newId: Id): GameAction = copy(id = newId)
}
