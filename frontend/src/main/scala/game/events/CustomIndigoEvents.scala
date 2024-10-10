package game.events

import indigo.shared.events.GlobalEvent
import gamelogic.gamestate.AddAndRemoveActions
import gamelogic.gamestate.GameAction
import gamelogic.abilities.Ability.AbilityId
import gamelogic.entities.Entity
import gamelogic.gameextras.GameMarkerInfo
import gamelogic.entities.Entity.Id

trait CustomIndigoEvents extends GlobalEvent

object CustomIndigoEvents {

  sealed trait BackendCommEvent extends CustomIndigoEvents
  object BackendCommEvent {
    case object EveryoneIsReady extends BackendCommEvent
  }

  sealed trait GameEvent extends CustomIndigoEvents
  object GameEvent {
    case class NewAction(action: GameAction)    extends GameEvent
    case class SendStartGame()                  extends GameEvent
    case class SendAction(action: GameAction)   extends GameEvent
    case class PutMarkers(info: GameMarkerInfo) extends GameEvent

    /** Sent within the game when an error message must be displayed on top of the screen. */
    case class ErrorMessage(message: String) extends GameEvent

    case class StartChoosingAbility(abilityId: AbilityId) extends GameEvent
    sealed trait TargetEvent extends GameEvent {
      def maybeTargetId: Option[Entity.Id]
    }
    case class ChooseTarget(entityId: Entity.Id) extends TargetEvent {
      def maybeTargetId: Option[Id] = Some(entityId)
    }
    case class ClearTarget() extends TargetEvent {
      def maybeTargetId: Option[Id] = None
    }

    /** Happens when the player toggle the target lock in mechanism */
    case class ToggleTargetLockIn() extends GameEvent
  }

  sealed trait UIEvent extends CustomIndigoEvents
  object UIEvent {}

}
