package services.database

import models.bff.ingame.{AllGameCredentials, GameCredentials}
import models.bff.outofgame.MenuGameWithPlayers
import services.crypto.Crypto
import zio.{Has, ZIO}

package object gamecredentials {

  type GameCredentialsDB = Has[GameCredentialsDB.Service]

  /**
    * Creates the game credentials and the credentials for all users, adds them to the database, and return
    * them for telling the users and launching the game server.
    */
  def createAndAddGameCredentials(
      gameInfo: MenuGameWithPlayers
  ): ZIO[GameCredentialsDB with Crypto, Throwable, AllGameCredentials] =
    ZIO.accessM(_.get[GameCredentialsDB.Service].createAndAddGameCredentials(gameInfo))

  /**
    * Removes all credentials related to the game with given id.
    */
  def removeAllGameCredentials(gameId: String): ZIO[GameCredentialsDB, Throwable, Unit] =
    ZIO.accessM(_.get[GameCredentialsDB.Service].removeAllGameCredentials(gameId))

  /** Checks that the game credentials are correct, and retrieve the credentials for the users of that game. */
  def retrieveUsersCredentials(
      gameCredentials: GameCredentials
  ): ZIO[GameCredentialsDB, Throwable, AllGameCredentials] =
    ZIO.accessM(_.get[GameCredentialsDB.Service].retrieveUsersCredentials(gameCredentials))

}
