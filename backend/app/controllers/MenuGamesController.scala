package controllers

import akka.actor.ActorRef
import dao.MenuGameDAO
import io.circe.generic.auto._
import javax.inject.{Inject, Named, Singleton}
import models.bff.outofgame.MenuGame
import models.common.PasswordWrapper
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.http.HttpErrorHandler
import play.api.mvc._
import services.config.Configuration
import services.crypto.Crypto
import services.database.db.Database.dbProvider
import services.database.gametables.GameTable
import services.logging.PlayLogging
import slick.jdbc.JdbcProfile
import utils.ReadsImplicits._
import utils.WriteableImplicits._
import services.actors.ActorProvider
import utils.playzio.PlayZIO._
import websocketkeepers.gamemenuroom.GameMenuRoomBookKeeper
import zio.UIO
import zio.clock.Clock

import scala.concurrent.ExecutionContext

@Singleton
final class MenuGamesController @Inject()(
    errorHandler: HttpErrorHandler,
    protected val dbConfigProvider: DatabaseConfigProvider,
    cc: ControllerComponents,
    @Named(GameMenuRoomBookKeeper.name) bookKeeper: ActorRef
)(implicit val ec: ExecutionContext)
    extends AbstractController(cc)
    with HasDatabaseConfigProvider[JdbcProfile] {

  lazy val logger: Logger = Logger("MenuGamesController")

  private val layer = Clock.live ++ Configuration.live ++ (dbProvider(db) >>> GameTable.live) ++ Crypto.live ++
    PlayLogging.live(logger) ++ ActorProvider.live(Map(GameMenuRoomBookKeeper.name -> bookKeeper))

  def newGame: Action[MenuGame] = Action.zio(parse.json[MenuGame])(
    MenuGameDAO.addNewGame.map(Ok(_)).provideButRequest[Request, MenuGame](layer)
  )

  def games: Action[AnyContent] = Action.zio(
    MenuGameDAO.games.map(Ok(_)).provideButRequest[Request, AnyContent](layer)
  )

  def joinGame(gameId: String): Action[PasswordWrapper] = Action.zio(parse.json[PasswordWrapper])(
    (MenuGameDAO.addPlayerToGame(gameId) *> UIO(Ok)).provideButRequest[Request, PasswordWrapper](layer)
  )

}
