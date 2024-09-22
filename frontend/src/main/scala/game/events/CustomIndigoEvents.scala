package game.events

import indigo.shared.events.GlobalEvent
import gamelogic.gamestate.AddAndRemoveActions
import gamelogic.gamestate.GameAction
import gamelogic.abilities.Ability.AbilityId
import gamelogic.entities.Entity

trait CustomIndigoEvents extends GlobalEvent

object CustomIndigoEvents {

  sealed trait BackendCommEvent extends CustomIndigoEvents
  object BackendCommEvent {
    case object EveryoneIsReady extends BackendCommEvent
  }

  sealed trait GameEvent extends CustomIndigoEvents
  object GameEvent {
    case class NewAction(action: GameAction)  extends GameEvent
    case class SendStartGame()                extends GameEvent
    case class SendAction(action: GameAction) extends GameEvent

    /** Sent within the game when an error message must be displayed on top of the screen. */
    case class ErrorMessage(message: String) extends GameEvent

    case class StartChoosingAbility(abilityId: AbilityId) extends GameEvent
    case class ChooseTarget(entityId: Entity.Id)          extends GameEvent
  }

  sealed trait UIEvent extends CustomIndigoEvents
  object UIEvent {

    /** Emitted by the ui parent every half a second so components can decide to slowly change */
    case class SlowFrameTick() extends UIEvent
  }

}
