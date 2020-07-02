package controllers

import dao.UserDAO
import io.circe.generic.auto._
import javax.inject.{Inject, Singleton}
import models.users.{LoginUser, NewUser}
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.http.HttpErrorHandler
import play.api.mvc._
import services.config.Configuration
import services.crypto.Crypto
import services.database.db.Database.dbProvider
import services.database.users.Users
import services.emails.JVMEmails
import services.logging.PlayLogging
import slick.jdbc.JdbcProfile
import utils.ReadsImplicits._
import utils.WriteableImplicits._
import utils.playzio.PlayZIO._
import zio.clock.Clock

import scala.concurrent.ExecutionContext

@Singleton
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
    PlayLogging.live(logger) ++ JVMEmails.live

  def users(from: Long, to: Long): Action[AnyContent] =
    Action.zio(UserDAO.allUsers(from, to).map(Ok(_)).provideButRequest[Request, AnyContent](layer))

  def register: Action[NewUser] = Action.zio(parse.json[NewUser])(
    UserDAO.register.provideButRequest[Request, NewUser](layer)
  )

  def confirmRegistration(registrationKey: String): Action[AnyContent] = Action.zio(
    UserDAO.confirmRegistration(registrationKey).provideLayer(layer)
  )

  /** This is going to be used only in development, while I don't have emails set up */
  def registrationKeyFromName(userName: String): Action[AnyContent] = Action.zio(
    UserDAO.registrationKeyFromName(userName).provideLayer(layer)
  )

  def login: Action[LoginUser] = Action.zio(parse.json[LoginUser])(
    UserDAO.login.provideButRequest[Request, LoginUser](layer)
  )

  def logout: Action[AnyContent] = Action { Ok.withNewSession }

  def me: Action[AnyContent] = Action.zio(UserDAO.me.provideButRequest[Request, AnyContent](layer))

  def amISuperUser: Action[AnyContent] = Action.zio(UserDAO.amISuperUser.provideButRequest[Request, AnyContent](layer))

}
