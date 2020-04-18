package dao

import guards.Guards
import models.bff.ingame.GameCredentialsWithGameInfo
import play.api.mvc.RequestHeader
import services.database.gamecredentials.GameCredentialsDB
import services.database.gametables._
import zio.{Has, ZIO}

object GameServerDAO {

  def retrieveCredentialsAndGameInfo
      : ZIO[GameTable with GameCredentialsDB with Has[RequestHeader], Throwable, GameCredentialsWithGameInfo] =
    for {
      credentials <- Guards.amIGameServer // guarding and retrieving credentials
      gameId = credentials.gameCredentials.gameId
      gameInfo <- gameWithPlayersById(gameId)
    } yield GameCredentialsWithGameInfo(credentials, gameInfo)

}
