package dao

import errors.ErrorADT
import guards.Guards._
import models.bff.outofgame.MenuGame
import play.api.mvc.{AnyContent, Request, Results}
import services.config.Configuration
import services.crypto.Crypto
import services.database.gametables._
import services.logging.{log, Logging}
import utils.playzio.HasRequest
import zio.clock.Clock
import zio.{Has, ZIO}
import io.circe.generic.auto._
import io.circe.syntax._

object MenuGameDAO extends Results {

  val games
      : ZIO[Logging with GameTable with Clock with Configuration with Has[HasRequest[Request, AnyContent]], ErrorADT, List[
        MenuGame
      ]] = (for {
    _ <- authenticated[AnyContent]
    allGames <- gameTables
    (errors, actualGames) = allGames.partitionMap(identity)
    _ <- ZIO.foreachParN(1)(errors.map(_.asJson.noSpaces))(log.warn(_))
  } yield actualGames).refineOrDie(ErrorADT.onlyErrorADT)

  val addNewGame: ZIO[Logging with GameTable with Crypto with Clock with Configuration with Has[
    HasRequest[Request, MenuGame]
  ], ErrorADT, Unit] = (for {
    sessionRequest <- authenticated[MenuGame]
    user = sessionRequest.user
    game = sessionRequest.body
    _ <- newGame(game.gameName, user.userId, game.maybeHashedPassword)
    _ <- log.info(s"New game ${game.gameName} created by ${user.userName}.")
  } yield ()).refineOrDie(ErrorADT.onlyErrorADT)

}
