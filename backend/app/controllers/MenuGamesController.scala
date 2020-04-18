package controllers

import akka.actor.typed.ActorRef
import dao.MenuGameDAO
import io.circe.generic.auto._
import javax.inject.{Inject, Singleton}
import models.bff.outofgame.MenuGame
import models.common.PasswordWrapper
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.mvc._
import services.actors.TypedActorProvider
import services.config.Configuration
import services.crypto.Crypto
import services.database.db.Database.dbProvider
import services.database.gametables.GameTable
import services.logging.PlayLogging
import slick.jdbc.JdbcProfile
import utils.ReadsImplicits._
import utils.WriteableImplicits._
import utils.playzio.PlayZIO._
import websocketkeepers.gameantichamber.JoinedGameDispatcherTyped
import websocketkeepers.gamemenuroom.GameMenuRoomBookKeeperTyped
import zio.UIO
import zio.clock.Clock

import scala.concurrent.ExecutionContext

@Singleton
final class MenuGamesController @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider,
    cc: ControllerComponents,
    //@Named(GameMenuRoomBookKeeper.name) bookKeeper: ActorRef
    gameMenuRoomBookKeeperRef: ActorRef[GameMenuRoomBookKeeperTyped.Message],
    joinedGameDispatcherRef: ActorRef[JoinedGameDispatcherTyped.Message]
)(implicit val ec: ExecutionContext)
    extends AbstractController(cc)
    with HasDatabaseConfigProvider[JdbcProfile] {

  lazy val logger: Logger = Logger("MenuGamesController")

  private val layer = Clock.live ++ Configuration.live ++ (dbProvider(db) >>> GameTable.live) ++ Crypto.live ++
    PlayLogging.live(logger) ++ TypedActorProvider.live(gameMenuRoomBookKeeperRef, joinedGameDispatcherRef)

  def newGame: Action[MenuGame] = Action.zio(parse.json[MenuGame])(
    MenuGameDAO.addNewGame.map(Ok(_)).provideButRequest[Request, MenuGame](layer)
  )

  def games: Action[AnyContent] = Action.zio(
    MenuGameDAO.games.map(Ok(_)).provideButRequest[Request, AnyContent](layer)
  )

  def joinGame(gameId: String): Action[PasswordWrapper] = Action.zio(parse.json[PasswordWrapper])(
    MenuGameDAO.addPlayerToGame(gameId).as(Ok).provideButRequest[Request, PasswordWrapper](layer)
  )

  def amIAmPlayingSomewhere: Action[AnyContent] = Action.zio {
    MenuGameDAO.amIAmPlayingSomewhere.map(Ok(_)).provideButRequest[Request, AnyContent](layer)
  }

  def amIInGame(gameId: String): Action[AnyContent] = Action.zio {
    MenuGameDAO.amIInGame(gameId).as(Ok).provideButRequest[Request, AnyContent](layer)
  }

  def gameInfo(gameId: String): Action[AnyContent] = Action.zio {
    MenuGameDAO.gameInfo(gameId).map(Ok(_)).provideButRequest[Request, AnyContent](layer)
  }

}
