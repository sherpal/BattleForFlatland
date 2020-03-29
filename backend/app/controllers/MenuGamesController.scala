package controllers

import dao.MenuGameDAO
import javax.inject.{Inject, Singleton}
import models.bff.outofgame.MenuGame
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.http.HttpErrorHandler
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Request}
import services.config.Configuration
import services.crypto.Crypto
import services.database.db.Database.dbProvider
import services.database.gametables.GameTable
import services.logging.PlayLogging
import slick.jdbc.JdbcProfile
import zio.clock.Clock
import utils.playzio.PlayZIO._
import io.circe.generic.auto._
import utils.ReadsImplicits._
import zio.UIO
import utils.WriteableImplicits._

import scala.concurrent.ExecutionContext

@Singleton
final class MenuGamesController @Inject()(
    errorHandler: HttpErrorHandler,
    protected val dbConfigProvider: DatabaseConfigProvider,
    cc: ControllerComponents
)(implicit val ec: ExecutionContext)
    extends AbstractController(cc)
    with HasDatabaseConfigProvider[JdbcProfile] {

  lazy val logger: Logger = Logger("MenuGamesController")

  private val layer = Clock.live ++ Configuration.live ++ (dbProvider(db) >>> GameTable.live) ++ Crypto.live ++
    PlayLogging.live(logger)

  def newGame: Action[MenuGame] = Action.zio(parse.json[MenuGame])(
    (MenuGameDAO.addNewGame *> UIO(Ok)).provideButRequest[Request, MenuGame](layer)
  )

  def games: Action[AnyContent] = Action.zio(
    MenuGameDAO.games.map(Ok(_)).provideButRequest[Request, AnyContent](layer)
  )

}
