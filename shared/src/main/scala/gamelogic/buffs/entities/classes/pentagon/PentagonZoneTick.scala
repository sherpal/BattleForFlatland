package gamelogic.buffs.entities.classes.pentagon

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, TickerBuff}
import gamelogic.entities.Entity
import gamelogic.entities.classes.pentagon.PentagonZone
import gamelogic.gamestate.gameactions.{EntityTakesDamage, RemoveEntity}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

final case class PentagonZoneTick(
    buffId: Buff.Id,
    bearerId: Entity.Id,
    appearanceTime: Long,
    lastTickTime: Long,
    duration: Long
) extends TickerBuff {
  def tickEffect(gameState: GameState, time: Long, idGenerator: IdGeneratorContainer): List[GameAction] =
    gameState.entityByIdAs[PentagonZone](bearerId).fold(List[GameAction]()) { zone =>
      gameState.allLivingEntities
        .filterNot(_.teamId == zone.teamId)
        .filter(_.collides(zone, time))
        .map(
          entity => EntityTakesDamage(idGenerator.gameActionIdGenerator(), time, entity.id, zone.damage, zone.sourceId)
        )
        .toList
    }

  val tickRate: Long = PentagonZone.tickRate

  def changeLastTickTime(time: Long): PentagonZoneTick = copy(lastTickTime = time)

  def resourceIdentifier: ResourceIdentifier = Buff.entitiesPentagonZoneBuff

  def initialActions(gameState: GameState, time: Long)(
      implicit idGeneratorContainer: IdGeneratorContainer
  ): List[GameAction] =
    Nil

  def endingAction(gameState: GameState, time: Long)(
      implicit idGeneratorContainer: IdGeneratorContainer
  ): List[GameAction] =
    List(RemoveEntity(idGeneratorContainer.gameActionIdGenerator(), time, bearerId))
}
