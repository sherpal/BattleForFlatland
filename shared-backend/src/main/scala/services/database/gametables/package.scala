package services.database

import errors.ErrorADT.InconsistentMenuGameInDB
import models.bff.outofgame.gameconfig.{GameConfiguration, PlayerInfo}
import models.bff.outofgame.{MenuGame, MenuGameWithPlayers}
import models.syntax.Pointed
import models.users.User
import services.crypto.Crypto
import zio.clock.Clock
import zio.{Has, ZIO}

package object gametables {

  type GameTable = Has[GameTable.Service]

  type GameTableTask[A] = ZIO[GameTable, Throwable, A]

  def gameTables: ZIO[GameTable, Throwable, List[Either[InconsistentMenuGameInDB, MenuGame]]] =
    ZIO.accessM(_.get[GameTable.Service].gameTables)

  def newGame(
      gameName: String,
      creatorId: String,
      creatorName: String,
      rawPassword: Option[String]
  )(
      implicit gameConfigurationPointed: Pointed[GameConfiguration]
  ): ZIO[GameTable with Crypto with Clock, Throwable, String] =
    ZIO.accessM(_.get[GameTable.Service].newGame(gameName, creatorId, creatorName, rawPassword))

  def gameExists(gameName: String): GameTableTask[Boolean] =
    ZIO.accessM(_.get[GameTable.Service].gameExists(gameName))

  def gameWithIdExists(gameId: String): GameTableTask[Boolean] =
    ZIO.accessM(_.get[GameTable.Service].gameWithIdExists(gameId))

  def selectGameByName(gameName: String): GameTableTask[Option[MenuGame]] =
    ZIO.accessM(_.get[GameTable.Service].selectGameByName(gameName))

  def selectGameById(gameId: String): GameTableTask[Option[MenuGame]] =
    ZIO.accessM(_.get[GameTable.Service].selectGameById(gameId))

  def deleteGame(gameName: String): GameTableTask[Int] = ZIO.accessM(_.get[GameTable.Service].deleteGame(gameName))

  def addUserToGame(
      user: User,
      gameId: String,
      maybePassword: Option[String]
  ): ZIO[GameTable with Clock with Crypto, Throwable, Int] =
    ZIO.accessM(_.get[GameTable.Service].addUserToGame(user, gameId, maybePassword))

  final def modifyPlayerInfo(gameId: String, user: User, playerInfo: PlayerInfo): ZIO[GameTable, Throwable, Unit] =
    ZIO.accessM(_.get[GameTable.Service].modifyPlayerInfo(gameId, user, playerInfo))

  def gameWithPlayersById(gameId: String): GameTableTask[MenuGameWithPlayers] =
    ZIO.accessM(_.get[GameTable.Service].gameWithPlayersById(gameId))

  def removePlayerFromGame(userId: String, gameId: String): GameTableTask[Boolean] =
    ZIO.accessM(_.get[GameTable.Service].removePlayerFromGame(userId, gameId))

  def isPlayerInGame(user: User, gameId: String): GameTableTask[Boolean] =
    ZIO.accessM(_.get[GameTable.Service].isPlayerInGame(user, gameId))

  def userAlreadyPlaying(userId: String): GameTableTask[Option[String]] =
    ZIO.accessM(_.get[GameTable.Service].userAlreadyPlaying(userId))

}
