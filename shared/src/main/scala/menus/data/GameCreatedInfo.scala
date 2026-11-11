package menus.data

import eventsourcing.{Effect, EntityInformation}
import io.circe.Codec

case class GameCreatedInfo(totalCreated: Int, totalStarted: Int) derives Codec

object GameCreatedInfo {

  enum Command:
    case NextId(replyTo: castor.Actor[Int])
    case GameWasStarted(replyTo: castor.Actor[Int])
    case This(replyTo: castor.Actor[GameCreatedInfo])

    def handle(state: GameCreatedInfo, id: Int): Effect[Event, GameCreatedInfo] =
      this match {
        case NextId(replyTo) =>
          Effect.Persist(Event.GameCreated()).thenReply(replyTo)(_.totalCreated)
        case GameWasStarted(replyTo) =>
          Effect.Persist(Event.GameStarted()).thenReply(replyTo)(_.totalStarted)
        case This(replyTo) => Effect.Ignore().thenReply(replyTo)(identity)
      }

  enum Event derives Codec:
    case GameCreated()
    case GameStarted()

    def apply(info: GameCreatedInfo): GameCreatedInfo = this match {
      case Event.GameCreated() => info.copy(totalCreated = info.totalCreated + 1)
      case Event.GameStarted() => info.copy(totalStarted = info.totalStarted + 1)
    }

  val entityInfo: EntityInformation[Command, Event, GameCreatedInfo] =
    EntityInformation.usingCirceSerialization[Command, Event, GameCreatedInfo](
      GameCreatedInfo(0, 0),
      _(_),
      (command, state, id) => command.handle(state, id)
    )

}
