package programs.frontend

import io.circe.generic.auto._
import models.bff.Routes._
import models.bff.outofgame.MenuGame
import services.http._
import zio.{UIO, _}
import zio.clock._
import zio.stream._

import scala.concurrent.duration._

package object games {

  val streamExpl: ZStream[Clock, Nothing, Int] =
    ZStream
      .fromSchedule(Schedule.spaced(zio.duration.Duration.fromScala(2.seconds)))
      .tap(x => UIO(println(x)))

  val downloadGames: URIO[HttpClient, Either[Throwable, List[MenuGame]]] = get[List[MenuGame]](allGames).either

  val loadGames: ZStream[HttpClient with Clock, Nothing, Either[Throwable, List[MenuGame]]] = ZStream
    .fromSchedule(Schedule.spaced(zio.duration.Duration.fromScala(5.seconds)))
    .flatMap(_ => ZStream.fromEffect(downloadGames))

}
