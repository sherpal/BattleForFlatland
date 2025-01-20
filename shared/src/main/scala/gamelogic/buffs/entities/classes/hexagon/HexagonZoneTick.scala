package gamelogic.buffs.entities.classes.hexagon

import gamelogic.buffs.Buff
import gamelogic.entities.Entity
import gamelogic.buffs.TickerBuff
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.GameState
import gamelogic.utils.IdGeneratorContainer
import gamelogic.entities.classes.hexagon.HexagonZone
import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.gamestate.gameactions.RemoveEntity
import gamelogic.gamestate.gameactions.EntityGetsHealed

final case class HexagonZoneTick(
    buffId: Buff.Id,
    bearerId: Entity.Id,
    appearanceTime: Long,
    lastTickTime: Long,
    duration: Long
) extends TickerBuff {
  def tickEffect(gameState: GameState, time: Long)(using IdGeneratorContainer): Vector[GameAction] =
    gameState.entityByIdAs[HexagonZone](bearerId).fold(Vector.empty[GameAction]) { zone =>
      gameState.allLivingEntities
        .filter(_.teamId == zone.teamId)
        .filter(_.collides(zone, time))
        .map(entity => EntityGetsHealed(genActionId(), time, entity.id, zone.heal, zone.sourceId))
        .toVector
    }

  val tickRate = HexagonZone.tickRate

  def changeLastTickTime(time: Long): HexagonZoneTick = copy(lastTickTime = time)

  def resourceIdentifier: ResourceIdentifier = Buff.entitiesHexagonZoneBuff

  def endingAction(gameState: GameState, time: Long)(using
      IdGeneratorContainer
  ): Vector[GameAction] =
    Vector(RemoveEntity(genActionId(), time, bearerId))
}
