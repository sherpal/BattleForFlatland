package game

import com.raquo.laminar.api.L._
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import models.bff.ingame.UserInput

import scala.Ordering.Double.TotalOrdering

/**
  * This class handles what happens when a player uses the "next target" input (tab by default).
  *
  * The following will happen:
  *
  * - a signal remembers what where the entity already affected by this, and the timestamp when it happened
  * - on the next "next target" input, we clear the list of all previously selected entities more than
  *   `timeBeforeLoopingBack` millis ago
  * - we look for all the remaining entities, and we select the closest one.
  *
  * If all entities where targeted within the last `timeBeforeLoopingBack` millis, then we simply target the one
  * which was the oldest.
  *
  * If, for some reason, there is no entity at all,
  */
final class NextTargetHandler(
    myId: Entity.Id,
    nextTargetEvents: EventStream[UserInput.NextTarget.type],
    gameStates: Signal[GameState],
    targetEntityWriter: Observer[Entity.Id],
    currentTime: () => Long
)(implicit owner: Owner) {

  /** An entity that was selected by "next target" will not be again within 5 seconds. */
  val timeBeforeLoopingBack: Long = 5000L

  /**
    * Remember the time at which a particular entity was last selected.
    * Equality on this class is done solely on the entity id basis.
    *
    * @param time time at which that entity was last selected
    * @param id entity of the id that was targeted
    */
  class WasTargetedInfo(val time: Long, val id: Entity.Id) {
    def tooOld(currentTime: Long): Boolean = currentTime - time > timeBeforeLoopingBack

    override def toString: String = s"WasTargetedInfo($time, $id)"

    override def equals(obj: Any): Boolean = obj match {
      case that: WasTargetedInfo => that.id == id
      case _                     => false
    }

    override def hashCode(): Int = id.hashCode()
  }

  /** Side effect-full! */
  nextTargetEvents
    .sample(gameStates)
    .map(gs => gs.players.get(myId) -> gs.allTargetableEntities.filter(_.teamId == Entity.teams.mobTeam).toSet)
    .collect { case (Some(me), possibleNextTargets) => (me, possibleNextTargets) }
    .fold((Map.empty[Entity.Id, WasTargetedInfo], Option.empty[Entity.Id])) {
      case ((previouslyTargeted, _), (me, possibleNextTargets)) =>
        // todo[enhance]: not really satisfied by the implementation.
        val possibleNextTargetsIds = possibleNextTargets.map(_.id)
        val now                    = currentTime()
        val tooRecentlyTargeted = previouslyTargeted
          .filterNot(_._2.tooOld(now))
          /* The following filter removes "dead" entities that were targeted recently. */
          .filter(wasTargeted => possibleNextTargetsIds.contains(wasTargeted._1))
        val tooRecentlyTargetedIds = tooRecentlyTargeted.keys.toSet
        val maybeNextTarget = possibleNextTargets
          .filter(entity => !tooRecentlyTargetedIds.contains(entity.id))
          .minByOption(_.pos distanceTo me.pos)
          .map(_.id)
          .orElse(tooRecentlyTargeted.valuesIterator.minByOption(_.time).map(_.id))

        (
          maybeNextTarget.fold(tooRecentlyTargeted)(id => tooRecentlyTargeted + (id -> new WasTargetedInfo(now, id))),
          maybeNextTarget
        )
    }
    .changes
    .collect { case (_, Some(nextTargetId)) => nextTargetId }
    .foreach(targetEntityWriter.onNext)
}
