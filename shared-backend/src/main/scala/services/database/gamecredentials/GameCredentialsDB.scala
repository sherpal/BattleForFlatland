package services.database.gamecredentials

import models.bff.ingame.{AllGameCredentials, GameCredentials, GameUserCredentials}
import models.bff.outofgame.MenuGameWithPlayers
import zio.{Task, UIO, ZIO, ZLayer}
import services.crypto._
import services.database.db.Database
import services.database.db.Database.DBProvider
import utils.database.DBProfile

object GameCredentialsDB {

  trait Service {

    /**
      * Add a new [[models.bff.ingame.GameCredentials]] to the database for the game-server to be read later.
      * It is assumed that a gameSecret already has been generated properly.
      */
    protected def addGameCredentials(gameCredentials: GameCredentials): Task[Int]

    /**
      * Remove the [[models.bff.ingame.GameCredentials]] with the specified game id from the database.
      */
    protected def removeGameCredentials(gameId: String): Task[Int]

    /**
      * Add the list of [[models.bff.ingame.GameUserCredentials]] to the database for the game-server to be
      * read later.
      * It is assumed that a userSecret already has been generated properly.
      */
    protected def addGameUserCredentials(credentials: List[GameUserCredentials]): Task[Option[Int]]

    /**
      * Remove all the [[models.bff.ingame.GameUserCredentials]] with the given game id from the database.
      */
    protected def removeGameUserCredentials(gameId: String): Task[Int]

    /**
      * Creates the game credentials and the credentials for all users, adds them to the database, and return
      * them for telling the users and launching the game server.
      */
    def createAndAddGameCredentials(gameInfo: MenuGameWithPlayers): ZIO[Crypto, Throwable, AllGameCredentials] =
      for {
        gameId <- UIO(gameInfo.game.gameId)
        gameSecret <- uuid
        gameCreds <- UIO(GameCredentials(gameId, gameSecret))
        allUsersCreds <- ZIO.foreach(gameInfo.players) { user =>
          uuid.map(secret => GameUserCredentials(user.userId, gameId, secret))
        }
        _ <- addGameCredentials(gameCreds)
        _ <- addGameUserCredentials(allUsersCreds)
      } yield AllGameCredentials(gameCreds, allUsersCreds)

    /**
      * Removes all credentials related to the game with given id.
      */
    def removeAllGameCredentials(gameId: String): ZIO[Any, Throwable, Unit] =
      for {
        _ <- removeGameCredentials(gameId)
        _ <- removeGameUserCredentials(gameId)
      } yield ()

  }

  def live: ZLayer[DBProvider, Nothing, GameCredentialsDB] =
    ZLayer.fromFunctionM(
      (dbProvider: Database.DBProvider) =>
        dbProvider.get[Database.Service].db.map(db => new GameCredentialsDBLive(DBProfile.api)(db))
    )

}
