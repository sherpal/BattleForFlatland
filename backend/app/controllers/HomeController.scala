package controllers

import errors.ErrorADT.{UserExists, YouAreUnauthorized}
import javax.inject._
import play.api.Configuration
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.http.HttpErrorHandler
import play.api.mvc._
import slick.jdbc.JdbcProfile
import utils.WriteableImplicits._
import utils.playzio.PlayZIO._
import zio.ZIO

import scala.concurrent.ExecutionContext

@Singleton
final class HomeController @Inject()(
    assets: Assets,
    errorHandler: HttpErrorHandler,
    config: Configuration,
    protected val dbConfigProvider: DatabaseConfigProvider,
    cc: ControllerComponents
)(implicit val ec: ExecutionContext)
    extends AbstractController(cc)
    with HasDatabaseConfigProvider[JdbcProfile] {

  def index: Action[AnyContent] = assets.at("index.html")

  def assetOrDefault(resource: String): Action[AnyContent] =
    if (resource.startsWith(config.get[String]("apiPrefix"))) {
      Action.async(r => errorHandler.onClientError(r, NOT_FOUND, "Not found"))
    } else {
      if (resource.contains(".")) assets.at(resource) else index
    }

  def hello(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok("Hello from play!")
  }

  def helloNbr(nbr: Int): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(s"You gave me $nbr")
  }

  def todo: Action[AnyContent] = TODO

  def adminFilter[R <: Request[_]](req: R) =
    for {
      _ <- ZIO.succeed(())
      stuff = req.session.get("stuff")
      _ <- if (stuff.isEmpty) ZIO.fail(YouAreUnauthorized) else ZIO.succeed(())
    } yield stuff.get

  case class SessionRequest[A](userName: String, request: Request[A]) extends WrappedRequest[A](request)

  def sessionTransformer[A, R <: Request[A]](req: R) =
    for {
      _ <- ZIO.succeed(())
      maybeUserName = req.session.get("userName")
      sessionRequest <- maybeUserName match {
        case None           => ZIO.fail(UserExists("coucou"))
        case Some(userName) => ZIO.succeed(SessionRequest(userName, req))
      }
    } yield sessionRequest

  def isAdmin = Action.zio(
    for {
      req <- zioRequest[Request, AnyContent]
      sessionRequest <- sessionTransformer[AnyContent, Request[AnyContent]](req)
      stuff <- adminFilter(sessionRequest)
    } yield Ok(stuff)
  )

}
