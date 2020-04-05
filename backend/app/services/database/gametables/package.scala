package services.database

import errors.ErrorADT.InconsistentMenuGameInDB
import models.bff.outofgame.MenuGame
import services.crypto.Crypto
import zio.clock.Clock
import zio.{Has, ZIO}

package object gametables {

  type GameTable = Has[GameTable.Service]

  def gameTables: ZIO[GameTable, Throwable, List[Either[InconsistentMenuGameInDB, MenuGame]]] =
    ZIO.accessM(_.get[GameTable.Service].gameTables)

  def newGame(
      gameName: String,
      creatorId: String,
      rawPassword: Option[String]
  ): ZIO[GameTable with Crypto with Clock, Throwable, Int] =
    ZIO.accessM(_.get[GameTable.Service].newGame(gameName, creatorId, rawPassword))

  def gameExists(gameName: String): ZIO[GameTable, Throwable, Boolean] =
    ZIO.accessM(_.get[GameTable.Service].gameExists(gameName))

  def selectGameByName(gameName: String): ZIO[GameTable, Throwable, Option[MenuGame]] =
    ZIO.accessM(_.get[GameTable.Service].selectGameByName(gameName))

}
