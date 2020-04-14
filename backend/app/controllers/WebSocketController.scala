package controllers

import akka.actor.ActorSystem
import akka.actor.typed.{ActorRef, Scheduler}
import akka.stream.scaladsl.Flow
import guards.WebSocketGuards
import javax.inject.{Inject, Named, Singleton}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.streams.ActorFlow
import play.api.mvc.{AbstractController, ControllerComponents, WebSocket}
import services.config.Configuration
import services.database.db.Database.dbProvider
import services.database.gametables.GameTable
import slick.jdbc.JdbcProfile
import utils.playzio.PlayZIO._
import utils.streams.TypedActorFlow
import websocketkeepers.gameantichamber.JoinedGameDispatcher
import websocketkeepers.gamemenuroom.{
  GameMenuClient,
  GameMenuClientTyped,
  GameMenuRoomBookKeeper,
  GameMenuRoomBookKeeperTyped
}
import services.crypto._
import zio.clock.Clock

import scala.concurrent.ExecutionContext

@Singleton
final class WebSocketController @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider,
    cc: ControllerComponents,
//    @Named(GameMenuRoomBookKeeper.name) gameMenuRoomBookKeeper: ActorRef,
//    @Named(JoinedGameDispatcher.name) joinedGameDispatcher: ActorRef
    gameMenuRoomBookKeeperRef: ActorRef[GameMenuRoomBookKeeperTyped.Message]
)(implicit val ec: ExecutionContext, actorSystem: ActorSystem)
    extends AbstractController(cc)
    with HasDatabaseConfigProvider[JdbcProfile] {

  private val layer = Clock.live ++ Configuration.live ++ (dbProvider(db) >>> GameTable.live) ++ Crypto.live

  def socketTest: WebSocket = WebSocket.accept[String, String](
    _ => Flow[String].wireTap(println(_))
  )

  //ActorFlow.actorRef()

  def gameMenuRoom: WebSocket = WebSocket.zio[String, String] {
    (for {
      user <- WebSocketGuards.authenticated
      id <- uuid
    } yield Flow[String]
      .map(_ => GameMenuClientTyped.Dummy)
      .via(
        TypedActorFlow
          .actorRef[GameMenuClientTyped.Message, String](
            out => GameMenuClientTyped(out, gameMenuRoomBookKeeperRef),
            "GameMenuRoom" + (user.userId + "_" + id).filterNot(_ == '-')
          )
      ))
      .provideButHeader(layer)
  }

}
