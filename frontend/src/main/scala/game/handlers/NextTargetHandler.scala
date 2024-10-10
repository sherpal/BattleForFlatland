package game.handlers

import gamelogic.entities.Entity
import indigo.*
import gamelogic.gamestate.GameState

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import game.events.CustomIndigoEvents
import gamelogic.entities.classes.PlayerClass
import models.bff.ingame.Controls.InputCode
import game.scenes.ingame.InGameScene.StartupData
import scala.collection.mutable
import models.bff.ingame.UserInput

/** This class handles what happens when a player uses the "next target" input (tab by default).
  *
  * The following will happen:
  *
  *   - we remember what where the entity already affected by this, and the timestamp when it
  *     happened
  *   - on the next "next target" input, we clear the list of all previously selected entities more
  *     than `timeBeforeLoopingBack` millis ago
  *   - we look for all the remaining entities, and we select the closest one.
  *
  * If all entities where targeted within the last `timeBeforeLoopingBack` millis, then we simply
  * target one "at random".
  *
  * If, for some reason, there is no entity at all, we simply do nothing
  */
class NextTargetHandler(myId: Entity.Id) {
  import NextTargetHandler.RecentTargetInfo

  private val recentTargetInfo: mutable.Map[Entity.Id, RecentTargetInfo] = mutable.Map.empty

  def handleKeyUpEvent(
      keyup: KeyboardHandler.RichKeyboardEvent[KeyboardEvent.KeyUp],
      gameState: GameState,
      context: FrameContext[StartupData]
  ): js.Array[CustomIndigoEvents.GameEvent.ChooseTarget] =
    if keyup.isUserInput(UserInput.NextTarget)
    then
      gameState.players
        .get(myId)
        .fold(js.Array())(nextTarget(_, gameState, context.gameTime.running))
    else js.Array()

  private def nextTarget(
      me: PlayerClass,
      gameState: GameState,
      now: Seconds
  ): js.Array[CustomIndigoEvents.GameEvent.ChooseTarget] = {
    val possibleNextTargets =
      gameState.allTargetableEntities.filter(_.teamId == Entity.teams.mobTeam).toSet

    val possibleNextTargetsIds = possibleNextTargets.map(_.id)

    // removing "dead" entities
    recentTargetInfo.filterInPlace((id, _) => possibleNextTargetsIds.contains(id))

    val entitiesThatWereTargetedTooRecently =
      recentTargetInfo.values.toJSArray.filterNot(_.tooOld(now))

    val tooRecentlyTargetedIds = entitiesThatWereTargetedTooRecently.map(_.entityId).toSet
    val maybeNextTarget = possibleNextTargets
      .filter(entity => !tooRecentlyTargetedIds.contains(entity.id))
      .minByOption(_.pos.distanceTo(me.pos))
      .map(_.id)
      .orElse(recentTargetInfo.values.minByOption(_.timestamp.toDouble).map(_.entityId))
      .toJSArray

    maybeNextTarget.foreach { entityId =>
      recentTargetInfo.update(entityId, RecentTargetInfo(entityId, now))
    }

    maybeNextTarget.map(CustomIndigoEvents.GameEvent.ChooseTarget(_))
  }

}

object NextTargetHandler {

  /** An entity that was selected by "next target" will not be again within 5 seconds. */
  private val timeBeforeLoopingBack: Seconds = Seconds(5)

  /** Remember the time at which a particular entity was last selected. Equality on this class is
    * done solely on the entity id basis.
    *
    * @param timestamp
    *   time at which that entity was last selected
    * @param entityId
    *   entity of the id that was targeted
    */
  private case class RecentTargetInfo(entityId: Entity.Id, timestamp: Seconds) {
    def tooOld(currentTime: Seconds): Boolean = currentTime - timestamp > timeBeforeLoopingBack
  }

}
