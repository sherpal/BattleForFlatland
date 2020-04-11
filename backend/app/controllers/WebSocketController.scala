package controllers

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.scaladsl.Flow
import guards.WebSocketGuards
import javax.inject.{Inject, Named, Singleton}
import models.bff.gameantichamber
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.streams.ActorFlow
import play.api.mvc.WebSocket.MessageFlowTransformer
import play.api.mvc.{AbstractController, ControllerComponents, WebSocket}
import services.config.Configuration
import services.database.db.Database.dbProvider
import services.database.gametables.GameTable
import slick.jdbc.JdbcProfile
import utils.ReadsImplicits._
import utils.playzio.PlayZIO._
import websocketkeepers.gameantichamber.{AntiChamberClient, JoinedGameDispatcher}
import websocketkeepers.gamemenuroom.{GameMenuClient, GameMenuRoomBookKeeper}
import zio.clock.Clock

import scala.concurrent.ExecutionContext

@Singleton
final class WebSocketController @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider,
    cc: ControllerComponents,
    @Named(GameMenuRoomBookKeeper.name) gameMenuRoomBookKeeper: ActorRef,
    @Named(JoinedGameDispatcher.name) joinedGameDispatcher: ActorRef
)(implicit val ec: ExecutionContext, actorSystem: ActorSystem)
    extends AbstractController(cc)
    with HasDatabaseConfigProvider[JdbcProfile] {

  private val layer = Clock.live ++ Configuration.live ++ (dbProvider(db) >>> GameTable.live)

  def socketTest: WebSocket = WebSocket.accept[String, String](
    _ => Flow[String].wireTap(println(_))
  )

  def gameMenuRoom: WebSocket = WebSocket.zio[String, String] {
    WebSocketGuards.authenticated
      .as(ActorFlow.actorRef[String, String](out => GameMenuClient.props(out, gameMenuRoomBookKeeper)))
      .provideButHeader(layer)
  }

  type AntiChamberProtocol = gameantichamber.WebSocketProtocol

  implicit private def antiChamberFlowTransformer: MessageFlowTransformer[AntiChamberProtocol, AntiChamberProtocol] =
    MessageFlowTransformer.jsonMessageFlowTransformer[AntiChamberProtocol, AntiChamberProtocol]

  /**
    * Joins the game anti chamber actor system when a Web Socket connects.
    * We check that the user is authenticated and then create the flow based on the actor system.
    */
  def gameAntiChamber(gameId: String): WebSocket =
    WebSocket.zio[AntiChamberProtocol, AntiChamberProtocol](
      WebSocketGuards
        .partOfGame(gameId)
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
