package dao

import akka.actor.ActorSystem
import akka.actor.typed.Scheduler
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, Uri}
import dao.GameAntiChamberDAO.askGameAntiChamberManager
import errors.ErrorADT
import errors.ErrorADT.{CouldNotFetchTokenFromGameServer, RawInternalError}
import guards.Guards
import io.circe.syntax._
import models.bff.ingame.{GameCredentialsWithGameInfo, GameUserCredentials}
import play.api.mvc.{Request, RequestHeader}
import services.actors.TypedActorProvider.TypedActorProvider
import services.config.Configuration
import services.database.gamecredentials.GameCredentialsDB
import services.database.gametables._
import utils.playzio.HasRequest
import websocketkeepers.gameantichamber.GameAntiChamberTyped.GameCredentialsWrapper
import zio.clock.Clock
import zio.{Has, UIO, ZIO}
import utils.ziohelpers.failIfWith

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Success

object GameServerDAO {

  def retrieveCredentialsAndGameInfo(
      implicit scheduler: Scheduler
  ): ZIO[TypedActorProvider with GameTable with GameCredentialsDB with Has[RequestHeader], Throwable, GameCredentialsWithGameInfo] =
    for {
      credentials <- Guards.amIGameServer // guarding and retrieving credentials
      gameId = credentials.gameCredentials.gameId
      gameInfo <- gameWithPlayersById(gameId)
      gameAntiChamberManagerRef <- askGameAntiChamberManager(gameId)
      _ <- ZIO.effectTotal(gameAntiChamberManagerRef ! GameCredentialsWrapper(credentials))
    } yield GameCredentialsWithGameInfo(credentials, gameInfo)

  def clientFetchGameServerToken(
      implicit actorSystem: ActorSystem
  ): ZIO[Clock with Configuration with Has[HasRequest[Request, GameUserCredentials]], Throwable, String] =
    for {
      sessionRequest <- Guards.authenticated[GameUserCredentials]
      credentials <- UIO(sessionRequest.body)
      token <- ZIO.fromFuture { _ =>
        implicit val ec: ExecutionContext = actorSystem.dispatcher
        Http()
          .singleRequest(
            HttpRequest(
              HttpMethods.POST,
              uri    = Uri("http://localhost:22222/api/token"), // todo!: unhardcode this!
              entity = credentials.asJson.noSpaces
            )
          )
          .flatMap(_.entity.toStrict(2.seconds))
          .map(_.data.utf8String)
      }
      _ <- failIfWith(token.isEmpty, CouldNotFetchTokenFromGameServer)
    } yield token

  def cancelGame(
      implicit actorSystem: ActorSystem
  ): ZIO[Clock with Configuration with Has[HasRequest[Request, GameUserCredentials]], ErrorADT, Boolean] =
    for {
      sessionRequest <- Guards.authenticated[GameUserCredentials]
      credentials <- UIO(sessionRequest.body)
      isSuccess <- ZIO
        .fromFuture { _ =>
          implicit val ec: ExecutionContext = actorSystem.dispatcher
          Http()
            .singleRequest(
              HttpRequest(
                HttpMethods.POST,
                uri    = Uri("http://localhost:22222/stop"), // todo!: un-hardcode this
                entity = credentials.asJson.noSpaces
              )
            )
            .andThen {
              case Success(response: HttpResponse) => response.entity.discardBytes()
            }
        }
        .bimap(t => RawInternalError(t.getMessage), _.status.isSuccess)
    } yield isSuccess

}
