package services.database.gametables

import errors.ErrorADT.{GameExists, InconsistentMenuGameInDB}
import models.bff.outofgame.{DBMenuGame, MenuGame}
import services.crypto._
import services.database.db.Database
import services.database.db.Database.DBProvider
import utils.database.DBProfile
import utils.ziohelpers._
import zio.clock._
import zio.{Task, ZIO, ZLayer}

object GameTable {

  trait Service {

    def gameTables: Task[List[Either[InconsistentMenuGameInDB, MenuGame]]]

    /**
      * Adds the [[models.bff.outofgame.DBMenuGame]] to the database.
      */
    protected def newDBGame(dbMenuGame: DBMenuGame): Task[Int]

    /** Returns the [[models.bff.outofgame.MenuGame]] with the given name if it exists. */
    def selectGameByName(gameName: String): Task[Option[MenuGame]]

    /** Returns whether the game with given name exists. */
    final def gameExists(gameName: String): Task[Boolean] = selectGameByName(gameName).map(_.isDefined)

    /**
      * Creates a new [[models.bff.outofgame.DBMenuGame]] in database using the given information.
      * Generates the game id and created on timestamp on site.
      *
      * The `rawPassword` is the one entered by the user when creating the game. /!\ Must be hashed! /!\
      * If it is None, then the game is set without any password.
      */
    final def newGame(
        gameName: String,
        creatorId: String,
        rawPassword: Option[String]
    ): ZIO[Crypto with Clock, Throwable, Int] =
      for {
        alreadyExistsFiber <- gameExists(gameName).fork
        hashed <- (for {
          password <- ZIO.fromOption(rawPassword)
          hashedPassword <- hashPassword(password)
        } yield hashedPassword.pw).option
        now <- currentDateTime.map(_.toLocalDateTime)
        id <- uuid
        alreadyExists <- alreadyExistsFiber.join
        _ <- failIfWith(alreadyExists, GameExists(gameName))
        dbGame = DBMenuGame(id, gameName, hashed, creatorId, now)
        added <- newDBGame(dbGame)
      } yield added

    def deleteGame(gameName: String): Task[Int]

  }

  def live: ZLayer[DBProvider, Nothing, GameTable] =
    ZLayer.fromFunctionM(
      (dbProvider: Database.DBProvider) => dbProvider.get.db.map(db => new GameTablesLive(DBProfile.api)(db))
    )

}
