package programs.frontend

import errors.ErrorADT
import io.circe.generic.auto._
import models.bff.Routes._
import models.bff.outofgame.MenuGame
import models.common.PasswordWrapper
import services.http._
import zio.{UIO, _}
import zio.clock._
import zio.stream._
import services.routing._

import scala.concurrent.duration._
import utils.ziohelpers.unsuccessfulStatusCode

package object games {

  val streamExpl: ZStream[Clock, Nothing, Int] =
    ZStream
      .fromSchedule(Schedule.spaced(zio.duration.Duration.fromScala(2.seconds)))
      .tap(x => UIO(println(x)))

  val downloadGames: ZIO[HttpClient, ErrorADT, List[MenuGame]] =
    get[List[MenuGame]](allGames).refineOrDie(ErrorADT.onlyErrorADT)

  val loadGames: ZStream[HttpClient with Clock, Nothing, Either[ErrorADT, List[MenuGame]]] = ZStream
    .fromSchedule(Schedule.spaced(zio.duration.Duration.fromScala(5.seconds)))
    .flatMap(_ => ZStream.fromEffect(downloadGames.either))

  def createNewGame(game: MenuGame): ZIO[HttpClient, Throwable, String] = post[MenuGame, String](newMenuGame, game)

  def joinGameProgram(game: MenuGame, maybePassword: PasswordWrapper): ZIO[Routing with HttpClient, Throwable, Int] =
    for {
      statusCode <- postIgnore(joinGame, joinGameParam, maybePassword)(game.gameId)
      _ <- unsuccessfulStatusCode(statusCode)
      _ <- moveTo(gameJoined ? gameJoinedParam)(game.gameId)
    } yield statusCode

}
