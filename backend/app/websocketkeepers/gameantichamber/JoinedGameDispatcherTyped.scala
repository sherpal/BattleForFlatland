package websocketkeepers.gameantichamber

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.google.inject.Provides
import javax.inject.Inject
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.ActorModule
import services.actors.{ActorProvider, TypedActorProvider}
import services.config.Configuration
import services.crypto.Crypto
import services.database.db.Database.dbProvider
import services.database.gametables.GameTable
import services.logging.{Logging, PlayLogging}
import slick.jdbc.JdbcProfile
import websocketkeepers.gamemenuroom.GameMenuRoomBookKeeperTyped
import zio.{Has, ZLayer}
import zio.clock.Clock

object JoinedGameDispatcherTyped extends ActorModule {

  import GameAntiChamberTyped._

  sealed trait Message

  case class NewClient(gameId: String, userId: String, ref: ActorRef[AntiChamberClientTyped.WebSocketProtocolWrapper])
      extends Message
  case class DidNotClose(ref: ActorRef[PlayerConnected]) extends Message
  case class CancelGame(ref: ActorRef[YouCanCleanUpCancel.type]) extends Message

  @Provides
  def apply(
      dbConfigProvider: DatabaseConfigProvider,
      gameMenuBookKeeperRef: ActorRef[GameMenuRoomBookKeeperTyped.Message]
  ): Behavior[Message] = Behaviors.setup { context =>
    def db = dbConfigProvider.get[JdbcProfile].db

    val logger: Logger = Logger("GameAntiChamberActorSystem")

    val layer
        : ZLayer[Any, Nothing, Clock with Configuration with GameTable with Crypto with Has[Logging.Service] with Has[
          TypedActorProvider.Service
        ]] =
      Clock.live ++ Configuration.live ++ (dbProvider(db) >>> GameTable.live) ++ Crypto.live ++
        PlayLogging.live(logger) ++ TypedActorProvider.live(gameMenuBookKeeperRef, context.self)

    Behaviors.receiveMessage { message =>
      println(message)
      Behaviors.same
    }
  }

}
