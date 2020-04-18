package controllers

import akka.actor.ActorSystem
import akka.actor.typed.{ActorRef, Scheduler}
import akka.stream.scaladsl.Flow
import dao.GameAntiChamberDAO
import errors.ErrorADT
import guards.WebSocketGuards
import io.circe.parser.decode
import io.circe.syntax._
import javax.inject.{Inject, Singleton}
import models.bff.gameantichamber
import models.bff.gameantichamber.WebSocketProtocol
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.mvc.WebSocket.MessageFlowTransformer
import play.api.mvc._
import services.actors.TypedActorProvider
import services.config.Configuration
import services.crypto._
import services.database.db.Database.dbProvider
import services.database.gamecredentials.GameCredentialsDB
import services.database.gametables.GameTable
import services.logging.PlayLogging
import slick.jdbc.JdbcProfile
import utils.ReadsImplicits._
import utils.playzio.PlayZIO._
import utils.streams.TypedActorFlow
import websocketkeepers.gameantichamber.{AntiChamberClientTyped, JoinedGameDispatcherTyped}
import websocketkeepers.gamemenuroom.GameMenuRoomBookKeeperTyped
import zio.UIO
import zio.clock.Clock

import scala.concurrent.ExecutionContext

@Singleton
final class GameAntiChamberController @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider,
    cc: ControllerComponents,
    typedJoinedGameDispatcherRef: ActorRef[JoinedGameDispatcherTyped.Message],
    typedGameMenuRoomBookKeeperRef: ActorRef[GameMenuRoomBookKeeperTyped.Message]
)(implicit val ec: ExecutionContext, actorSystem: ActorSystem, scheduler: Scheduler)
    extends AbstractController(cc)
    with HasDatabaseConfigProvider[JdbcProfile] {

  lazy val logger: Logger = Logger("GameAntiChamberController")

  private val layer = Clock.live ++
    Configuration.live ++
    (dbProvider(db) >>> (GameTable.live ++ GameCredentialsDB.live)) ++
    Crypto.live ++
    PlayLogging.live(logger) ++
    TypedActorProvider.live(
      typedGameMenuRoomBookKeeperRef,
      typedJoinedGameDispatcherRef
    )

  type AntiChamberProtocol = gameantichamber.WebSocketProtocol

  implicit private def antiChamberFlowTransformer: MessageFlowTransformer[AntiChamberProtocol, AntiChamberProtocol] =
    MessageFlowTransformer.jsonMessageFlowTransformer[AntiChamberProtocol, AntiChamberProtocol]

  def cancelGame(gameId: String): Action[AnyContent] = Action.zio {
    (GameAntiChamberDAO.cancelGame(gameId).refineOrDie(ErrorADT.onlyErrorADT) *> UIO(Ok))
      .provideButRequest[Request, AnyContent](layer)
  }

  def launchGame(gameId: String): Action[AnyContent] = Action.zio {
    GameAntiChamberDAO
      .startGame(gameId)
      .refineOrDie(ErrorADT.onlyErrorADT)
      .as(Ok)
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
    WebSocket.zio[String, String](
      (for {
        user <- WebSocketGuards
          .partOfGame(gameId)
        id <- uuid
      } yield Flow[String]
        .map(decode[WebSocketProtocol])
        .collect { case Right(protocol) => protocol }
        .map(AntiChamberClientTyped.WebSocketProtocolWrapper(_, AntiChamberClientTyped.OutsideWorldSender))
        .via(
          TypedActorFlow.actorRef[AntiChamberClientTyped.Message, WebSocketProtocol](
            outerWorld => AntiChamberClientTyped(outerWorld, typedJoinedGameDispatcherRef, gameId, user),
            "AntiChamber" + List(gameId, user.userId, id).map(_.filterNot(_ == '-')).mkString("_")
          )
        )
        .map(_.asJson.noSpaces)).provideButHeader(layer)
    )

}
