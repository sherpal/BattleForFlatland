package services.database.gametables

import errors.ErrorADT.{
  GameDoesNotExist,
  GameExists,
  InconsistentMenuGameInDB,
  IncorrectGamePassword,
  UserAlreadyPlaying
}
import models.bff.outofgame.{DBMenuGame, MenuGame, MenuGameWithPlayers}
import models.users.User
import services.crypto._
import services.database.db.Database
import services.database.db.Database.DBProvider
import utils.database.DBProfile
import utils.database.models.UserInGameTable
import utils.ziohelpers._
import zio.clock._
import zio.{Task, UIO, ZIO, ZLayer}

object GameTable {

  trait Service {

    def gameTables: Task[List[Either[InconsistentMenuGameInDB, MenuGame]]]

    /**
      * Adds the [[models.bff.outofgame.DBMenuGame]] to the database.
      */
    protected def newDBGame(dbMenuGame: DBMenuGame): Task[Int]

    /** Returns the [[models.bff.outofgame.MenuGame]] with the given name if it exists. */
    def selectGameByName(gameName: String): Task[Option[MenuGame]]

    /** Returns the [[models.bff.outofgame.MenuGame]] with the given Id if it exists. */
    def selectGameById(gameId: String): Task[Option[MenuGame]]

    /** Returns whether the game with given name exists. */
    final def gameExists(gameName: String): Task[Boolean] = selectGameByName(gameName).map(_.isDefined)

    /** Returns whether the game with the given id exists. */
    final def gameWithIdExists(gameId: String): Task[Boolean] = selectGameById(gameId).map(_.isDefined)

    /**
      * Adds the instance of [[utils.database.models.UserInGameTable]] to the database.
      * Data are assumed to be correct (with the correct `joinedOn` timestamp)
      */
    protected def addUsersInGameTables(userInGameTable: UserInGameTable): Task[Int]

    /**
      * Deletes from the database the row with the same `userId` and `gameId` as the provided
      * [[utils.database.models.UserInGameTable]].
      *
      * Note that, technically, userId column should be unique. Not the case, though, because we will enforce it in the
      * code. //todo[think]: think then because it should be unique
      */
    protected def removeUsersInGameTables(userInGameTable: UserInGameTable): Task[Int]

    /**
      * Returns whether the player with the given id is already linked to a game.
      * If it is, returns the game id wrapped in some. If not, returns None.
      */
    def userAlreadyPlaying(userId: String): Task[Option[String]]

    /**
      * Returns the list of users in the game with that id.
      */
    protected def playersInGameWithId(gameId: String): Task[List[User]]

    /** Returns whether this user belongs to the game with that id. */
    final def isPlayerInGame(user: User, gameId: String): Task[Boolean] =
      playersInGameWithId(gameId).map(_.exists(_.userId == user.userId))

    /**
      * Creates a new [[models.bff.outofgame.DBMenuGame]] in database using the given information.
      * Generates the game id and created on timestamp on site.
      * Returns the generated id.
      *
      * The `rawPassword` is the one entered by the user when creating the game. /!\ Must be hashed! /!\
      * If it is None, then the game is set without any password.
      */
    final def newGame(
        gameName: String,
        creatorId: String,
        creatorName: String,
        rawPassword: Option[String]
    ): ZIO[Crypto with Clock, Throwable, String] =
      for {
        userPlayingFiber <- userAlreadyPlaying(creatorId).fork
        alreadyExistsFiber <- gameExists(gameName).fork
        hashed <- (for {
          password <- ZIO.fromOption(rawPassword)
          hashedPassword <- hashPassword(password)
        } yield hashedPassword.pw).option
        now <- currentDateTime.map(_.toLocalDateTime)
        id <- uuid
        alreadyExists <- alreadyExistsFiber.join
        _ <- failIfWith(alreadyExists, GameExists(gameName))
        userPlaying <- userPlayingFiber.join
        _ <- failIfWith(userPlaying.isDefined, UserAlreadyPlaying(creatorName))
        dbGame = DBMenuGame(id, gameName, hashed, creatorId, now)
        _ <- newDBGame(dbGame)
      } yield id

    def deleteGame(gameName: String): Task[Int]

    /**
      * Adds the given user to the game.
      */
    final def addUserToGame(
        user: User,
        gameId: String,
        maybePassword: Option[String]
    ): ZIO[Clock with Crypto, Throwable, Int] =
      for {
        maybeGameFiber <- selectGameById(gameId).fork
        userAlreadyThere <- userAlreadyPlaying(user.userId)
        _ <- failIfWith(userAlreadyThere.isDefined, UserAlreadyPlaying(user.userName))
        maybeGame <- maybeGameFiber.join
        game <- getOrFail(maybeGame, GameDoesNotExist(gameId))
        passwordIsValid <- checkPasswordIfRequired(maybePassword, game.maybeHashedPassword.map(HashedPassword))
        _ <- failIfWith(!passwordIsValid, IncorrectGamePassword)
        now <- currentDateTime.map(_.toLocalDateTime)
        userInGameTable <- UIO(UserInGameTable(gameId, user.userId, now))
        added <- addUsersInGameTables(userInGameTable)
      } yield added

    /**
      * Fetches game and players information for the game id.
      */
    final def gameWithPlayersById(gameId: String): Task[MenuGameWithPlayers] =
      for {
        maybeGameFiber <- selectGameById(gameId).fork
        playersFiber <- playersInGameWithId(gameId).fork
        maybeGame <- maybeGameFiber.join
        game <- getOrFail(maybeGame, GameDoesNotExist(gameId))
        players <- playersFiber.join

      } yield MenuGameWithPlayers(game, players)

    /**
      * Remove the user with given id from the game with given Id.
      * If the user was the creator, we also delete the game.
      * Returning true in that case, false otherwise, so that other players can be notified.
      */
    final def removePlayerFromGame(userId: String, gameId: String): Task[Boolean] =
      for {
        maybeGame <- selectGameById(gameId)
        game <- ZIO.fromOption(maybeGame).flatMapError(_ => UIO(GameDoesNotExist(gameId)))
        _ <- if (game.gameCreator.userId == userId) deleteGame(game.gameName)
        else
          removeUsersInGameTables(
            UserInGameTable.now(gameId, userId)
          )
      } yield game.gameCreator.userId == userId

  }

  def live: ZLayer[DBProvider, Nothing, GameTable] =
    ZLayer.fromFunctionM(
      (dbProvider: Database.DBProvider) =>
        dbProvider.get[Database.Service].db.map(db => new GameTablesLive(DBProfile.api)(db))
    )

}
