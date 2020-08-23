package gamelogic.buffs.boss.boss102

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, TickerBuff}
import gamelogic.entities.Entity
import gamelogic.entities.boss.boss102.DamageZone
import gamelogic.gamestate.gameactions.{EntityTakesDamage, RemoveEntity}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

final case class DamageZoneTick(buffId: Buff.Id, bearerId: Entity.Id, appearanceTime: Long, lastTickTime: Long)
    extends TickerBuff {
  def tickEffect(gameState: GameState, time: Long, idGenerator: IdGeneratorContainer): List[GameAction] =
    gameState.entities.get(bearerId).collect { case zone: DamageZone => zone }.fold(List[GameAction]()) { zone =>
      gameState.players.valuesIterator
        .filter(_.collides(zone, time))
        .map { player =>
          EntityTakesDamage(
            idGenerator.gameActionIdGenerator(),
            time,
            player.id,
            DamageZoneTick.damageOnTick,
            zone.sourceId
          )
        }
        .toList
    }

  val tickRate: Long = DamageZoneTick.tickRate

  def changeLastTickTime(time: Long): DamageZoneTick = copy(lastTickTime = time)

  def duration: Long = -1

  def resourceIdentifier: ResourceIdentifier = Buff.boss102DamageZoneBuff

  def endingAction(gameState: GameState, time: Long)(
      implicit idGeneratorContainer: IdGeneratorContainer
  ): List[GameAction] =
    List(RemoveEntity(idGeneratorContainer.gameActionIdGenerator(), time, bearerId))
}

object DamageZoneTick {

  final def tickRate: Long = 500L

  final def damageOnTick: Double = 5.0

}
