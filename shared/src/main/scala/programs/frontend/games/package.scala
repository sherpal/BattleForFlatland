package programs.frontend

import errors.ErrorADT
import io.circe.generic.auto._
import models.bff.Routes._
import models.bff.outofgame.MenuGame
import services.http._
import zio.{UIO, _}
import zio.clock._
import zio.stream._

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

  def createNewGame(game: MenuGame): ZIO[HttpClient, Throwable, Int] =
    for {
      statusCode <- postIgnore(newMenuGame, game)
      - <- unsuccessfulStatusCode(statusCode)
    } yield statusCode

}
