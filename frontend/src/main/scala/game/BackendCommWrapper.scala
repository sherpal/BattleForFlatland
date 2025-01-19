package game

import com.raquo.laminar.api.A.*
import gamelogic.gamestate.AddAndRemoveActions
import models.bff.ingame.InGameWSProtocol
import gamelogic.gamestate.ActionGatherer
import game.scenes.ingame.InGameScene.InGameModel

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import indigo.shared.collections.Batch
import org.scalajs.dom.CustomEvent
import game.events.CustomIndigoEvents

class BackendCommWrapper(
    allPlayersAreReadyEvents: EventStream[Unit],
    actionsFromServerEvents: EventStream[AddAndRemoveActions],
    val sendMessageToBackend: InGameWSProtocol.Outgoing => Unit
)(using Owner) {
  private var actionsUpdate       = AddAndRemoveActions.empty
  private var _allPlayersAreReady = false

  def theActionsUpdate = actionsUpdate

  actionsFromServerEvents.foreach { next =>
    actionsUpdate = actionsUpdate.composeWithNext(next)
  }
  allPlayersAreReadyEvents.foreach { _ =>
    println("All ready!")
    _allPlayersAreReady = true
  }

  def transform(model: InGameModel): (InGameModel, Batch[CustomIndigoEvents.GameEvent.NewAction]) =
    if actionsUpdate.nonEmpty then
      val updates = actionsUpdate
      actionsUpdate = AddAndRemoveActions.empty
      (
        model.withActionGatherer(
          model.actionGatherer.slaveAddAndRemoveActions(
            updates.actionsToAdd,
            updates.oldestTimeToRemove,
            updates.idsOfActionsToRemove
          ),
          model.unconfirmedActions.lastOption.toVector
        ),
        Batch.fromIndexedSeq(updates.actionsToAdd.map(CustomIndigoEvents.GameEvent.NewAction(_)))
      )
    else (model, Batch.empty)

  def allPlayersAreReady: Boolean = _allPlayersAreReady

}
