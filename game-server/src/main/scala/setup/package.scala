import akka.actor
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import errors.ErrorADT
import io.circe.generic.auto._
import io.circe.parser.decode
import models.bff.ingame.{GameCredentials, GameCredentialsWithGameInfo}
import services.database.gametables._
import utils.customheader.{GameServerIdHeader, GameServerSecretHeader}
import zio.ZIO
import zio.console.{putStrLn, Console}

import scala.concurrent.duration._

package object setup {

  def fetchGameInfo(gameId: String): ZIO[Console with GameTable, Throwable, Unit] =
    for {
      game <- gameWithPlayersById(gameId)
      _ <- putStrLn(game.toString)
    } yield ()

  def fetchGameInfo(
      gameCredentials: GameCredentials,
      actorSystem: ActorSystem[_]
  ): ZIO[Any, Throwable, GameCredentialsWithGameInfo] = {
    implicit val classicSystem: actor.ActorSystem = actorSystem.toClassic
    for {
      response <- ZIO.fromFuture { _ =>
        Http().singleRequest(
          HttpRequest(
            uri = "http://localhost:9000/game-server/game-info", // todo: un-hardcode this
            headers = List(
              new GameServerIdHeader(gameCredentials.gameId),
              new GameServerSecretHeader(gameCredentials.gameSecret)
            )
          )
        )
      }
      responseBody <- ZIO.fromFuture { implicit ec =>
        response.entity.toStrict(1.second)
      }
      info <- ZIO
        .fromEither(decode[GameCredentialsWithGameInfo](responseBody.data.utf8String))
        .mapError(ErrorADT.fromCirceDecodingError)
    } yield info
  }

}
