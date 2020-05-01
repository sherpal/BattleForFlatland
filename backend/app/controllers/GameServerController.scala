package controllers

import akka.actor.ActorSystem
import akka.actor.typed.{ActorRef, Scheduler}
import dao.GameServerDAO
import errors.ErrorADT
import io.circe.generic.auto._
import javax.inject.Inject
import models.bff.ingame.GameUserCredentials
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.mvc._
import services.actors.TypedActorProvider
import services.config.Configuration
import services.crypto.Crypto
import services.database.db.Database.dbProvider
import services.database.gamecredentials.GameCredentialsDB
import services.database.gametables.GameTable
import services.logging.PlayLogging
import slick.jdbc.JdbcProfile
import utils.ReadsImplicits._
import utils.WriteableImplicits._
import utils.playzio.PlayZIO._
import websocketkeepers.gameantichamber.JoinedGameDispatcherTyped
import websocketkeepers.gamemenuroom.GameMenuRoomBookKeeperTyped
import zio.ZLayer
import zio.clock.Clock

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

  def clientFetchToken: Action[GameUserCredentials] = Action.zio(parse.json[GameUserCredentials]) {
    GameServerDAO.clientFetchGameServerToken
      .map(Ok(_))
      .refineOrDie(ErrorADT.onlyErrorADT)
      .provideButRequest[Request, GameUserCredentials](layer)
  }

}
