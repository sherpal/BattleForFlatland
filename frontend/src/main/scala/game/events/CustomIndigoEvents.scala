package game.events

import indigo.shared.events.GlobalEvent
import gamelogic.gamestate.AddAndRemoveActions
import gamelogic.gamestate.GameAction

trait CustomIndigoEvents extends GlobalEvent

object CustomIndigoEvents {

  sealed trait BackendCommEvent extends CustomIndigoEvents
  object BackendCommEvent {
    case object EveryoneIsReady extends BackendCommEvent
  }

  sealed trait GameEvent extends GlobalEvent
  object GameEvent {
    case class NewAction(action: GameAction) extends GameEvent
  }

}
