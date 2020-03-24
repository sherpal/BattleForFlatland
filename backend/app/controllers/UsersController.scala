package controllers

import dao.UserDAO
import errors.ErrorADT
import guards.Guards
import javax.inject.Inject
import models.users.{LoginUser, NewUser}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.http.HttpErrorHandler
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Request}
import slick.jdbc.JdbcProfile
import utils.playzio.PlayZIO._
import utils.ReadsImplicits._
import io.circe.generic.auto._
import play.api.Logger
import zio.clock.Clock
import services.config.Configuration
import services.database.users.Users
import services.database.db.Database.dbProvider
import services.crypto.Crypto
import services.logging.PlayLogging

import scala.concurrent.ExecutionContext

final class UsersController @Inject()(
    assets: Assets,
    errorHandler: HttpErrorHandler,
    //config: Configuration,
    protected val dbConfigProvider: DatabaseConfigProvider,
    cc: ControllerComponents
)(implicit val ec: ExecutionContext)
    extends AbstractController(cc)
    with HasDatabaseConfigProvider[JdbcProfile] {

  lazy val logger: Logger = Logger("UsersController")

  private val layer = Clock.live ++ Configuration.live ++ (dbProvider(db) >>> Users.live) ++ Crypto.live ++
    PlayLogging.live(logger)

  def register: Action[NewUser] = Action.zio(parse.json[NewUser])(
    UserDAO.register.provideButRequest[Request, NewUser](layer)
  )

  def confirmRegistration(registrationKey: String): Action[AnyContent] = Action.zio(
    UserDAO.confirmRegistration(registrationKey).provideLayer(layer)
  )

  def login: Action[LoginUser] = Action.zio(parse.json[LoginUser])(
    UserDAO.login.provideButRequest[Request, LoginUser](layer)
  )

  def logout: Action[AnyContent] = Action { Ok.withNewSession }

  def amISuperUser: Action[AnyContent] = Action.zio(UserDAO.amISuperUser.provideButRequest[Request, AnyContent](layer))

}
