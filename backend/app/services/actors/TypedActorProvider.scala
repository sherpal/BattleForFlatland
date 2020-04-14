package services.actors

import akka.actor.typed.ActorRef
import websocketkeepers.gameantichamber.JoinedGameDispatcherTyped
import websocketkeepers.gamemenuroom.GameMenuRoomBookKeeperTyped
import zio._

object TypedActorProvider {

  trait Service {

    def gameMenuRoomBookKeeperRef: UIO[ActorRef[GameMenuRoomBookKeeperTyped.Message]]

    def joinedGameDispatcherRef: UIO[ActorRef[JoinedGameDispatcherTyped.Message]]

  }

  type TypedActorProvider = Has[Service]

  def gameMenuRoomBookKeeperRef: URIO[TypedActorProvider, ActorRef[GameMenuRoomBookKeeperTyped.Message]] = ZIO.accessM(
    _.get[Service].gameMenuRoomBookKeeperRef
  )

  def joinedGameDispatcherRef: URIO[TypedActorProvider, ActorRef[JoinedGameDispatcherTyped.Message]] = ZIO.accessM(
    _.get[Service].joinedGameDispatcherRef
  )

  def live(
      bookKeeperRef: ActorRef[GameMenuRoomBookKeeperTyped.Message],
      gameDispatcherRef: ActorRef[JoinedGameDispatcherTyped.Message]
  ): Layer[Nothing, Has[Service]] = ZLayer.succeed {
    new Service {
      def gameMenuRoomBookKeeperRef: UIO[ActorRef[GameMenuRoomBookKeeperTyped.Message]] = UIO(bookKeeperRef)

      def joinedGameDispatcherRef: UIO[ActorRef[JoinedGameDispatcherTyped.Message]] = UIO(gameDispatcherRef)
    }
  }

}
