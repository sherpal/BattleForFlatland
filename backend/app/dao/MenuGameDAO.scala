package dao

import errors.ErrorADT
import guards.Guards._
import io.circe.generic.auto._
import io.circe.syntax._
import models.bff.outofgame.MenuGame
import models.syntax.Validated
import play.api.mvc.{AnyContent, Request, Results}
import services.config.Configuration
import services.crypto.Crypto
import services.database.gametables._
import services.logging.{log, Logging}
import utils.actors.ActorProvider
import utils.actors.ActorProvider.ActorProvider
import utils.playzio.HasRequest
import utils.ziohelpers.fieldsValidateOrFail
import websocketkeepers.gamemenuroom.GameMenuRoomBookKeeper
import zio.clock.Clock
import zio.{Has, ZIO}

object MenuGameDAO extends Results {

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
  ], ErrorADT, Unit] = (for {
    sessionRequest <- authenticated[MenuGame]
    user = sessionRequest.user
    game = sessionRequest.body
    _ <- fieldsValidateOrFail(Validated[MenuGame, ErrorADT].fieldsValidator)(game)
    _ <- newGame(game.gameName, user.userId, game.maybeHashedPassword)
    _ <- log.info(s"New game ${game.gameName} created by ${user.userName}.")
    menuGameBookKeeper <- ActorProvider.actorRef(GameMenuRoomBookKeeper.name)
    _ <- ZIO
      .effect(menuGameBookKeeper.get ! GameMenuRoomBookKeeper.NewGame)
      .either // either cause we don't care if it fails
  } yield ()).refineOrDie(ErrorADT.onlyErrorADT)

}
