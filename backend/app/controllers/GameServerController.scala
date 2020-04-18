package controllers

import akka.actor.ActorSystem
import akka.actor.typed.{ActorRef, Scheduler}
import dao.GameServerDAO
import errors.ErrorADT
import javax.inject.Inject
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Request, RequestHeader}
import services.actors.TypedActorProvider
import services.config.Configuration
import services.crypto.Crypto
import services.database.db.Database.dbProvider
import services.database.gamecredentials.GameCredentialsDB
import services.database.gametables.GameTable
import services.logging.PlayLogging
import slick.jdbc.JdbcProfile
import websocketkeepers.gameantichamber.JoinedGameDispatcherTyped
import websocketkeepers.gamemenuroom.GameMenuRoomBookKeeperTyped
import zio.clock.Clock
import utils.playzio.PlayZIO._
import io.circe.generic.auto._
import utils.WriteableImplicits._
import zio.ZLayer

import scala.concurrent.ExecutionContext

/**
  * Here we implement routes that, technically, only a game server will have access to, because it is the only
  * place that will have knowledge of the credentials required.
  */
final class GameServerController @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider,
    cc: ControllerComponents,
    typedJoinedGameDispatcherRef: ActorRef[JoinedGameDispatcherTyped.Message],
    typedGameMenuRoomBookKeeperRef: ActorRef[GameMenuRoomBookKeeperTyped.Message]
)(implicit val ec: ExecutionContext, actorSystem: ActorSystem, scheduler: Scheduler)
    extends AbstractController(cc)
    with HasDatabaseConfigProvider[JdbcProfile] {

  lazy val logger: Logger = Logger("GameServerController")

  private val layer = Clock.live ++
    Configuration.live ++
    (dbProvider(db) >>> (GameTable.live ++ GameCredentialsDB.live)) ++
    Crypto.live ++
    PlayLogging.live(logger) ++
    TypedActorProvider.live(
      typedGameMenuRoomBookKeeperRef,
      typedJoinedGameDispatcherRef
    )

  /**
    * This route requires to have headers for game id and game secret.
    * These are represented by the classes
    * [[utils.customheader.GameServerSecretHeader]] and [[utils.customheader.GameServerIdHeader]].
    */
  def fetchGameInfoAndCredentials: Action[AnyContent] = Action.zio {
    zioRequest[Request, AnyContent]
      .flatMap(
        request =>
          GameServerDAO.retrieveCredentialsAndGameInfo
            .map(Ok(_))
            .refineOrDie(ErrorADT.onlyErrorADT)
            .provideLayer(layer ++ ZLayer.succeed(request: RequestHeader))
      )

  }

}
