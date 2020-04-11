package guards

import errors.ErrorADT
import errors.ErrorADT.YouAreNotInGame
import models.users.User
import play.api.mvc.RequestHeader
import services.config.Configuration
import services.database.gametables._
import utils.playzio.PlayZIO._
import utils.ziohelpers.failIfWith
import zio.clock.Clock
import zio.{Has, ZIO}

object WebSocketGuards {

  def authenticated: ZIO[Clock with Configuration with Has[RequestHeader], ErrorADT, User] =
    for {
      header <- zioRequestHeader
      user <- Guards.userFromRequestHeader(header)
    } yield user

  def partOfGame(gameId: String): ZIO[GameTable with Clock with Configuration with Has[RequestHeader], ErrorADT, User] =
    for {
      user <- authenticated
      isInGame <- isPlayerInGame(user, gameId).refineOrDie(ErrorADT.onlyErrorADT)
      _ <- failIfWith(!isInGame, YouAreNotInGame(gameId))
    } yield user

}
