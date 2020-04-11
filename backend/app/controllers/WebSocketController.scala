package controllers

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.scaladsl.Flow
import guards.WebSocketGuards
import javax.inject.{Inject, Named, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import play.api.http.HttpErrorHandler
import play.api.libs.streams.ActorFlow
import play.api.mvc.{AbstractController, ControllerComponents, WebSocket}
import websocketkeepers.gamemenuroom.{GameMenuClient, GameMenuRoomBookKeeper}
import utils.ReadsImplicits._
import models.bff.gameantichamber
import play.api.mvc.WebSocket.MessageFlowTransformer
import services.config.Configuration
import zio.clock.Clock
import utils.playzio.PlayZIO._
import websocketkeepers.gameantichamber.{AntiChamberClient, JoinedGameDispatcher}

import scala.concurrent.ExecutionContext

@Singleton
final class WebSocketController @Inject()(
    assets: Assets,
    errorHandler: HttpErrorHandler,
    //config: Configuration,
    protected val dbConfigProvider: DatabaseConfigProvider,
    cc: ControllerComponents,
    @Named(GameMenuRoomBookKeeper.name) gameMenuRoomBookKeeper: ActorRef,
    @Named(JoinedGameDispatcher.name) joinedGameDispatcher: ActorRef
)(implicit val ec: ExecutionContext, actorSystem: ActorSystem)
    extends AbstractController(cc) {

  private val layer = Clock.live ++ Configuration.live

  def socketTest: WebSocket = WebSocket.accept[String, String](
    _ => Flow[String].wireTap(println(_))
  )

  def gameMenuRoom: WebSocket = WebSocket.accept[String, String] { _ =>
    ActorFlow.actorRef[String, String](out => GameMenuClient.props(out, gameMenuRoomBookKeeper))
  }

  type AntiChamberProtocol = gameantichamber.WebSocketProtocol

  implicit private def antiChamberFlowTransformer: MessageFlowTransformer[AntiChamberProtocol, AntiChamberProtocol] =
    MessageFlowTransformer.jsonMessageFlowTransformer[AntiChamberProtocol, AntiChamberProtocol]

  def gameAntiChamber(gameId: String): WebSocket =
    WebSocket.zio[AntiChamberProtocol, AntiChamberProtocol](
      WebSocketGuards.authenticated
        .map { user =>
          ActorFlow.actorRef[AntiChamberProtocol, AntiChamberProtocol](
            out =>
              AntiChamberClient.props(
                out,
                joinedGameDispatcher,
                gameId,
                user
              )
          )
        }
        .provideButHeader(layer)
    )

}
