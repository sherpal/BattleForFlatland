package controllers

import akka.actor.{ActorRef, ActorSystem}
import dao.GameAntiChamberDAO
import errors.ErrorADT
import guards.WebSocketGuards
import javax.inject.{Inject, Named, Singleton}
import models.bff.gameantichamber
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.streams.ActorFlow
import play.api.mvc.WebSocket.MessageFlowTransformer
import play.api.mvc._
import services.actors.ActorProvider
import services.config.Configuration
import services.crypto.Crypto
import services.database.db.Database.dbProvider
import services.database.gametables.GameTable
import services.logging.PlayLogging
import slick.jdbc.JdbcProfile
import utils.ReadsImplicits._
import utils.playzio.PlayZIO._
import websocketkeepers.gameantichamber.{AntiChamberClient, JoinedGameDispatcher}
import zio.UIO
import zio.clock.Clock

import scala.concurrent.ExecutionContext

@Singleton
final class GameAntiChamberController @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider,
    cc: ControllerComponents,
    @Named(JoinedGameDispatcher.name) joinedGameDispatcher: ActorRef
)(implicit val ec: ExecutionContext, actorSystem: ActorSystem)
    extends AbstractController(cc)
    with HasDatabaseConfigProvider[JdbcProfile] {

  lazy val logger: Logger = Logger("GameAntiChamberController")

  private val layer = Clock.live ++ Configuration.live ++ (dbProvider(db) >>> GameTable.live) ++ Crypto.live ++
    PlayLogging.live(logger) ++ ActorProvider.live(Map(JoinedGameDispatcher.name -> joinedGameDispatcher))

  type AntiChamberProtocol = gameantichamber.WebSocketProtocol

  implicit private def antiChamberFlowTransformer: MessageFlowTransformer[AntiChamberProtocol, AntiChamberProtocol] =
    MessageFlowTransformer.jsonMessageFlowTransformer[AntiChamberProtocol, AntiChamberProtocol]

  def cancelGame(gameId: String): Action[AnyContent] = Action.zio {
    (GameAntiChamberDAO.cancelGame(gameId).refineOrDie(ErrorADT.onlyErrorADT) *> UIO(Ok))
      .provideButRequest[Request, AnyContent](layer)
  }

  def iAmStillThere(gameId: String): Action[AnyContent] = Action.zio {
    (GameAntiChamberDAO.iAmStillThere(gameId).refineOrDie(ErrorADT.onlyErrorADT) *> UIO(Ok))
      .provideButRequest[Request, AnyContent](layer)
  }

  def playerLeavesGame(gameId: String): Action[AnyContent] = Action.zio {
    (GameAntiChamberDAO.leaveGame(gameId).refineOrDie(ErrorADT.onlyErrorADT) *> UIO(Ok))
      .provideButRequest[Request, AnyContent](layer)
  }

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
