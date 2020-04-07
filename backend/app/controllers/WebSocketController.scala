package controllers

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.scaladsl.Flow
import javax.inject.{Inject, Named, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import play.api.http.HttpErrorHandler
import play.api.libs.streams.ActorFlow
import play.api.mvc.{AbstractController, ControllerComponents, WebSocket}
import websocketkeepers.gamemenuroom.{GameMenuClient, GameMenuRoomBookKeeper}

import scala.concurrent.ExecutionContext

@Singleton
final class WebSocketController @Inject()(
    assets: Assets,
    errorHandler: HttpErrorHandler,
    //config: Configuration,
    protected val dbConfigProvider: DatabaseConfigProvider,
    cc: ControllerComponents,
    @Named(GameMenuRoomBookKeeper.name) gameMenuRoomBookKeeper: ActorRef
)(implicit val ec: ExecutionContext, actorSystem: ActorSystem)
    extends AbstractController(cc) {

  def socketTest: WebSocket = WebSocket.accept[String, String](
    _ => Flow[String].wireTap(println(_))
  )

  def gameMenuRoom: WebSocket = WebSocket.accept[String, String] { _ =>
    ActorFlow.actorRef[String, String](out => GameMenuClient.props(out, gameMenuRoomBookKeeper))
  }

}
