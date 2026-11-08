package gamelogic.buffs.boss.spiritofnara

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, TickerBuff}
import gamelogic.entities.Entity
import gamelogic.entities.boss.spiritofnara.BlizzardCloud
import gamelogic.gamestate.gameactions.spiritofnara.PutChilled
import gamelogic.gamestate.gameactions.{EntityTakesDamage, RefreshMovementSpeed, RemoveEntity}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** Put on the Spirit of Nara for as long as its [[BlizzardCloud]] goes around the room.
  *
  * At each tick, the players under the storm take [[BlizzardTicks.damage]] and are [[Chilled]].
  * When the buff ends, the storm vanishes.
  */
final case class BlizzardTicks(
    buffId: Buff.Id,
    bearerId: Entity.Id,
    appearanceTime: Long,
    lastTickTime: Long,
    cloudId: Entity.Id
) extends TickerBuff {

  def tickEffect(gameState: GameState, time: Long)(using IdGeneratorContainer): Vector[GameAction] =
    gameState
      .entityByIdAs[BlizzardCloud](cloudId)
      .fold(Vector.empty[GameAction]) { cloud =>
        gameState.players.values.toVector
          .sortBy(_.id.value)
          .filter(player => cloud.covers(player.currentPosition(time), time))
          .flatMap { player =>
            Vector(
              EntityTakesDamage(genActionId(), time, player.id, BlizzardTicks.damage, bearerId),
              PutChilled(genActionId(), time, genBuffId(), player.id),
              RefreshMovementSpeed(genActionId(), time, player.id)
            )
          }
      }

  val tickRate: Long = BlizzardTicks.tickRate

  def changeLastTickTime(time: Long): BlizzardTicks = copy(lastTickTime = time)

  def duration: Long = BlizzardTicks.duration

  def resourceIdentifier: ResourceIdentifier = Buff.spiritOfNaraBlizzardTicks

  def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Entity.Id])(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector(RemoveEntity(genActionId(), time, cloudId))
}

object BlizzardTicks {
  inline def duration: Long = 25000L
  inline def tickRate: Long = 1000L
  inline def damage: Double = 11.0
}
