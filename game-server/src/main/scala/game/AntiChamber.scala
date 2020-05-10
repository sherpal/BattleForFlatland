package game

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import models.bff.ingame.InGameWSProtocol
import models.bff.outofgame.MenuGameWithPlayers

/**
  * Actor responsible for receiving messages when people are joining at the server launch.
  *
  * Once players have sync their clocks using the ping-pong mechanism, they sent a [[game.AntiChamber.Ready]] message
  * to this actor.
  *
  * Once all players are connected, the game master is notified, as well as the [[game.ActionTranslator]], so that
  * the former can know that it can launch the game, and the latter will be able to check integrity of incoming
  * messages.
  */
object AntiChamber {

  sealed trait Message

  case class GameInfo(menuGameWithPlayers: MenuGameWithPlayers) extends Message
  case class Ready(userId: String, actorRef: ActorRef[InGameWSProtocol]) extends Message
  case class ReadyToStart(userId: String) extends Message
  private case object ShouldWeStart extends Message

  def apply(
      gameMaster: ActorRef[GameMaster.Message],
      actionUpdateCollector: ActorRef[ActionUpdateCollector.Message]
  ): Behavior[Message] =
    waitingForGameInfo(gameMaster, actionUpdateCollector, Nil)

  private def waitingForPlayers(
      gameMaster: ActorRef[GameMaster.Message],
      menuGameWithPlayers: MenuGameWithPlayers,
      actionUpdateCollector: ActorRef[ActionUpdateCollector.Message],
      connected: Map[String, ActorRef[InGameWSProtocol]]
  ): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case Ready(userId, actorRef) =>
        // todo: warn the action update collector if a ref was replaced
        actionUpdateCollector ! ActionUpdateCollector.NewExternalMember(actorRef)

        context.self ! ShouldWeStart
        waitingForPlayers(gameMaster, menuGameWithPlayers, actionUpdateCollector, connected + (userId -> actorRef))
      case ReadyToStart(userId) =>
        gameMaster ! GameMaster.IAmReadyToStart(userId)
        Behaviors.same
      case ShouldWeStart if connected.size == menuGameWithPlayers.players.length =>
        gameMaster ! GameMaster.EveryoneIsReady(connected, menuGameWithPlayers)
        Behaviors.same
      case ShouldWeStart => // still waiting for players
        Behaviors.same
    }
  }

  private def waitingForGameInfo(
      gameMaster: ActorRef[GameMaster.Message],
      actionUpdateCollector: ActorRef[ActionUpdateCollector.Message],
      pendingReadyStates: List[Ready]
  ): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case GameInfo(menuGameWithPlayers) =>
        pendingReadyStates.foreach(context.self ! _)
        waitingForPlayers(gameMaster, menuGameWithPlayers, actionUpdateCollector, Map())
      case ready: Ready =>
        waitingForGameInfo(gameMaster, actionUpdateCollector, ready +: pendingReadyStates)
      case ShouldWeStart =>
        Behaviors.unhandled
      case ReadyToStart(_) =>
        // can't happen before going to waiting for players
        Behaviors.unhandled
    }

  }

}
