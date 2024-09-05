package application

import gamelogic.gamestate.GameAction
import models.bff.ingame.InGameWSProtocol
import gamelogic.gamestate.AddAndRemoveActions
import gamelogic.gamestate.GameState
import concurrent.ActionBuffer

class ActionTranslator(
    connectedPlayersInfo: Vector[ConnectedPlayerInfo],
    val actionBuffer: ActionBuffer[GameAction]
) {

  inline def newGameActions(from: String, actions: Vector[GameAction]) =
    actionBuffer.addActions(actions)

  inline def aiNewGameActions(actions: Vector[GameAction]) =
    actionBuffer.addActions(actions)

  private var actionsSubscribers: Vector[AddAndRemoveActions => Unit] = Vector.empty

  def subscribe(handler: AddAndRemoveActions => Unit): Unit =
    actionsSubscribers = actionsSubscribers :+ handler

  def dispatchGameActions(update: AddAndRemoveActions): Unit = if update.nonEmpty then {
    connectedPlayersInfo.foreach { info =>
      info.send(InGameWSProtocol.AddAndRemoveActions.fromGameLogic(update))
    }
    actionsSubscribers.foreach(_(update))
    println(s"Dispatching ${update.actionsToAdd}")
  }

  def dispatchGameState(gameState: GameState): Unit =
    // currently do nothing, we'll see when we have ais
    ()

}
