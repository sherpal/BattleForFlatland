package gamelogic.buffs.boss.spiritofnara

import gamelogic.abilities.Ability
import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, TickerBuff}
import gamelogic.entities.Entity
import gamelogic.entities.boss.spiritofnara.FlameWreathRing
import gamelogic.gamestate.gameactions.{EntityTakesDamage, RemoveBuff, RemoveEntity}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** Put on the Spirit of Nara for as long as its [[FlameWreathRing]]s burn.
  *
  * At each tick, it checks whether a player crossed one of the rings. If so, they all explode,
  * dealing [[FlameWreathWatch.explosionDamage]] to every player. When the buff ends, the rings
  * vanish.
  */
final case class FlameWreathWatch(
    buffId: Buff.Id,
    bearerId: Entity.Id,
    appearanceTime: Long,
    lastTickTime: Long
) extends TickerBuff {

  def tickEffect(gameState: GameState, time: Long)(using IdGeneratorContainer): Vector[GameAction] =
    if FlameWreathWatch.someoneCrossed(gameState, time) then
      gameState.players.keys.toVector.sortBy(_.value).map { playerId =>
        EntityTakesDamage(
          genActionId(),
          time,
          playerId,
          FlameWreathWatch.explosionDamage,
          bearerId,
          Ability.spiritOfNara.flameWreathId
        )
      } ++ removeRings(gameState, time) :+ RemoveBuff(genActionId(), time, bearerId, buffId)
    else Vector.empty

  val tickRate: Long = FlameWreathWatch.tickRate

  def changeLastTickTime(time: Long): FlameWreathWatch = copy(lastTickTime = time)

  def duration: Long = FlameWreathWatch.duration

  def resourceIdentifier: ResourceIdentifier = Buff.spiritOfNaraFlameWreathWatch

  def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Entity.Id])(using
      IdGeneratorContainer
  ): Vector[GameAction] = removeRings(gameState, time)

  private def removeRings(gameState: GameState, time: Long)(using
      IdGeneratorContainer
  ): Vector[GameAction] =
    gameState
      .allTEntities[FlameWreathRing]
      .keys
      .toVector
      .sortBy(_.value)
      .map(ringId => RemoveEntity(genActionId(), time, ringId))
}

object FlameWreathWatch {
  inline def duration: Long = 20000L

  /** Rings are checked often, so that the explosion closely follows the mistake. */
  inline def tickRate: Long = 200L

  inline def explosionDamage: Double = 55.0

  /** Whether a player is on the wrong side of one of the rings. */
  def someoneCrossed(gameState: GameState, time: Long): Boolean = {
    val rings = gameState.allTEntities[FlameWreathRing].values
    gameState.players.values.exists { player =>
      val position = player.currentPosition(time)
      rings.exists(_.isCrossedBy(player.id, position))
    }
  }
}
