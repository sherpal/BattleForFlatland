package gamelogic.gameextras

import scala.collection.mutable

import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.{GameAction, GameState}

/** The damage that the players dealt to each of their enemies, for the damage meter.
  *
  * Like the visual effects of the client, it is told about the actions received from the server,
  * and counts each one once, even if the server sends it again when it corrects the past. The
  * damage counted is the one that was really dealt, after the buffs of the target (like a shield on
  * the boss) transformed it.
  */
final class DamageTracker {

  /** What was dealt to one enemy: the damage of each player, and when it was first and last hit. */
  private final class Fight {
    val bySource: mutable.Map[Entity.Id, Double] = mutable.Map.empty
    var firstTime: Long                          = Long.MaxValue
    var lastTime: Long                           = Long.MinValue
  }

  private val fights    = mutable.Map.empty[Entity.Id, Fight]
  private val seen      = mutable.Set.empty[GameAction.Id]
  private val seenOrder = mutable.Queue.empty[GameAction.Id]

  /** Actions received from the server, already in `gameState`. */
  def received(actions: Iterable[GameAction], gameState: GameState): Unit = {
    def isPlayer(id: Entity.Id) =
      gameState.players.contains(id) || gameState.deadPlayers.contains(id)

    for case damage: EntityTakesDamage <- actions if seen.add(damage.id) do {
      seenOrder.enqueue(damage.id)
      if seenOrder.size > 5000 then seen.remove(seenOrder.dequeue())
      val dealt = gameState.applyActionChangers(damage).collect {
        case EntityTakesDamage(_, time, targetId, amount, sourceId)
            if isPlayer(sourceId) && !isPlayer(targetId) =>
          (targetId, sourceId, time, amount)
      }
      for (targetId, sourceId, time, amount) <- dealt do {
        val fight = fights.getOrElseUpdate(targetId, new Fight)
        fight.bySource.update(sourceId, fight.bySource.getOrElse(sourceId, 0.0) + amount)
        fight.firstTime = math.min(fight.firstTime, time)
        fight.lastTime = math.max(fight.lastTime, time)
      }
    }
  }

  /** The damage dealt to `targetId` by each player, the highest first. */
  def damageTo(targetId: Entity.Id): Vector[(Entity.Id, Double)] =
    fights.get(targetId).fold(Vector.empty)(_.bySource.toVector.sortBy(-_._2))

  /** When `targetId` was first and last hit by a player. */
  def hitTimes(targetId: Entity.Id): Option[(Long, Long)] =
    fights.get(targetId).map(fight => (fight.firstTime, fight.lastTime))

}
