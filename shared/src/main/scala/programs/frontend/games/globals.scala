package programs.frontend.games

import errors.ErrorADT
import io.circe.generic.auto.*
import models.bff.Routes.*
import models.bff.ingame.GameUserCredentials
import models.bff.outofgame.{MenuGame, MenuGameWithPlayers}
import models.common.PasswordWrapper
import models.users.RouteDefinitions.*
import services.http.*
import services.logging.*
import services.routing.*
import utils.ziohelpers.unsuccessfulStatusCode
import zio.stream.*
import zio.*

val streamExpl = ZStream.fromSchedule(Schedule.spaced(2.seconds))

val downloadGames: ZIO[HttpClient, ErrorADT, List[MenuGame]] =
  get[List[MenuGame]](allGames).refineOrDie(ErrorADT.onlyErrorADT)

val loadGames: ZStream[HttpClient & Clock, Nothing, Either[ErrorADT, List[MenuGame]]] = ZStream
  .fromSchedule(Schedule.spaced(5.seconds))
  .flatMap(_ => ZStream.fromZIO(downloadGames.either))

def createNewGame(game: MenuGame): ZIO[HttpClient, Throwable, String] = post[String](newMenuGame, game)

def joinGameProgram(game: MenuGame, maybePassword: PasswordWrapper): ZIO[Routing & HttpClient, Throwable, Int] =
  for {
    statusCode <- postIgnore(joinGame, joinGameParam, maybePassword)(game.gameId)
    _          <- unsuccessfulStatusCode(statusCode)
    _          <- moveTo(gameJoined ? gameIdParam)(game.gameId)
  } yield statusCode

val amIAmPlayingSomewhere: ZIO[Routing & HttpClient, Throwable, Unit] = for {
  maybeGameId <- get[Option[String]](amIPlaying)
  _ <- maybeGameId match {
    case Some(gameId) => moveTo(gameJoined ? gameIdParam)(gameId)
    case None         => ZIO.unit
  }
} yield ()

/** Fetch game information. If there is an error in the process, currently we assume that any error means that the
  * client should go back to the home page. // todo: store the error using the storage service // todo: refinement of
  * behaviour depending on the actual error.
  * @return
  *   the game information wrapped into an Option if it succeed, None otherwise.
  */
def fetchGameInfo(gameId: String): URIO[Routing & Logging & HttpClient, Option[MenuGameWithPlayers]] =
  get[MenuGameWithPlayers](gameInfo, gameIdParam)(gameId)
    .flatMapError(error =>
      for {
        _ <- log.error(error.toString).ignore
        _ <- moveTo(homeRoute)
      } yield ()
    )
    .option

def sendCancelGame(gameId: String): ZIO[HttpClient, ErrorADT, Int] =
  postIgnore(cancelGame, gameIdParam)(gameId).refineOrDie(ErrorADT.onlyErrorADT)

def sendLaunchGame(gameId: String): ZIO[HttpClient, ErrorADT, Int] =
  postIgnore(startGame, gameIdParam)(gameId).refineOrDie(ErrorADT.onlyErrorADT)

def pokingPresence(gameId: String): ZIO[Routing & HttpClient, ErrorADT, Int] =
  postIgnore(iAmStilThere, gameIdParam)(gameId)
    .refineOrDie(ErrorADT.onlyErrorADT)
    .tapError(_ => moveTo(homeRoute))

def iAmLeaving(gameId: String): ZIO[HttpClient, ErrorADT, Int] =
  postIgnore(leaveGame, gameIdParam)(gameId).refineOrDie(ErrorADT.onlyErrorADT)

/** Asks the game server for the token. */
// todo: change the host!!!
def fetchGameToken(credentials: GameUserCredentials): ZIO[HttpClient, ErrorADT, String] =
  post[String](gameServerToken, credentials)
    .refineOrDie(ErrorADT.onlyErrorADT)
