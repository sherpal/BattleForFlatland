package dao

import errors.ErrorADT
import guards.Guards._
import guards.WebSocketGuards
import io.circe.generic.auto._
import io.circe.syntax._
import models.bff.outofgame.{MenuGame, MenuGameWithPlayers}
import models.common.PasswordWrapper
import models.syntax.Validated
import models.users.User
import play.api.mvc.{AnyContent, Request, Results}
import services.config.Configuration
import services.crypto.Crypto
import services.database.gametables._
import services.logging.{log, Logging}
import services.actors.ActorProvider
import services.actors.ActorProvider.ActorProvider
import utils.playzio.HasRequest
import utils.ziohelpers.fieldsValidateOrFail
import websocketkeepers.gamemenuroom.GameMenuRoomBookKeeper
import zio.clock.Clock
import zio.{Has, UIO, ZIO}

object MenuGameDAO { //} extends Results {

  val games
      : ZIO[Logging with GameTable with Clock with Configuration with Has[HasRequest[Request, AnyContent]], ErrorADT, List[
        MenuGame
      ]] = (for {
    _ <- authenticated[AnyContent]
    allGames <- gameTables
    (errors, actualGames) = allGames.partitionMap(identity)
    gamesWithoutPasswords = actualGames.map(_.forgetPassword) // frontend doesn't need to know the password, even hashed
    _ <- ZIO.foreachParN(1)(errors.map(_.asJson.noSpaces))(log.warn(_))
  } yield gamesWithoutPasswords).refineOrDie(ErrorADT.onlyErrorADT)

  val addNewGame: ZIO[ActorProvider with Logging with GameTable with Crypto with Clock with Configuration with Has[
    HasRequest[Request, MenuGame]
  ], ErrorADT, String] = (for {
    sessionRequest <- authenticated[MenuGame]
    user = sessionRequest.user
    game = sessionRequest.body
    _ <- fieldsValidateOrFail(Validated[MenuGame, ErrorADT].fieldsValidator)(game)
    newGameId <- newGame(game.gameName, user.userId, user.userName, game.maybeHashedPassword)
    _ <- log.info(s"New game ${game.gameName} ($newGameId) created by ${user.userName}.")
    menuGameBookKeeper <- ActorProvider.actorRef(GameMenuRoomBookKeeper.name)
    _ <- ZIO
      .effect(menuGameBookKeeper.get ! GameMenuRoomBookKeeper.NewGame)
      .either // either cause we don't care if it fails
  } yield newGameId).refineOrDie(ErrorADT.onlyErrorADT)

  def addPlayerToGame(gameId: String): ZIO[GameTable with Clock with Crypto with Configuration with Has[
    HasRequest[Request, PasswordWrapper]
  ], ErrorADT, Unit] =
    (for {
      sessionRequest <- authenticated[PasswordWrapper]
      user                   = sessionRequest.user
      maybeSubmittedPassword = sessionRequest.body.submittedPassword
      _ <- addUserToGame(user, gameId, maybeSubmittedPassword)
    } yield ()).refineOrDie(ErrorADT.onlyErrorADT)

  def amIInGame(
      gameId: String
  ): ZIO[GameTable with Clock with Configuration with Has[HasRequest[Request, AnyContent]], ErrorADT, Int] =
    partOfGame[AnyContent](gameId).refineOrDie(ErrorADT.onlyErrorADT) *> UIO(0)

  def amIAmPlayingSomewhere
      : ZIO[GameTable with Clock with Configuration with Has[HasRequest[Request, AnyContent]], ErrorADT, Option[
        String
      ]] =
    (for {
      sessionRequest <- authenticated[AnyContent]
      maybeGameId <- userAlreadyPlaying(sessionRequest.user.userId)
    } yield maybeGameId).refineOrDie(ErrorADT.onlyErrorADT)

  def gameInfo(
      gameId: String
  ): ZIO[GameTable with Clock with Configuration with Has[HasRequest[Request, AnyContent]], ErrorADT, MenuGameWithPlayers] =
    partOfGame[AnyContent](gameId).refineOrDie(ErrorADT.onlyErrorADT).map(_.gameInfo)

}
