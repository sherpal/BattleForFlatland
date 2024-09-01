package services.menugames

import zio.*
import models.bff.outofgame.MenuGameWithPlayers
import menus.data.User
import errors.ErrorADT
import models.bff.outofgame.gameconfig.PlayerInfo
import models.bff.outofgame.gameconfig.GameConfiguration
import menus.data.AllGameCredentials

trait MenuGames {

  def menuGames: ZIO[Any, Nothing, Vector[MenuGameWithPlayers]]

  def createGame(
      gameName: String,
      maybeHashedPassword: Option[String],
      gameCreator: User
  ): ZIO[Any, Nothing, Either[ErrorADT, MenuGameWithPlayers]]

  def deleteGame(gameId: String): ZIO[Any, Nothing, Unit]

  def joinGame(
      user: User,
      gameId: String,
      maybePassword: Option[String]
  ): ZIO[Any, Nothing, Either[ErrorADT, Vector[MenuGameWithPlayers]]]

  def removePlayer(
      requester: User,
      gameId: String
  ): ZIO[Any, Nothing, Either[ErrorADT, MenuGameWithPlayers]]

  def changePlayerInfo(
      user: User,
      gameId: String,
      newPlayerInfo: PlayerInfo
  ): ZIO[Any, Nothing, Either[ErrorADT, MenuGameWithPlayers]]

  def changeGameMetadata(
      requester: User,
      gameId: String,
      gameMetadata: GameConfiguration.GameConfigMetadata
  ): ZIO[Any, Nothing, Either[ErrorADT, MenuGameWithPlayers]]

  def launchGame(
      requester: User,
      gameId: String
  ): ZIO[Any, Nothing, Either[ErrorADT, Unit]]

  def retrieveAllGameCredentials(
      gameId: String,
      secret: String
  ): ZIO[Any, Nothing, Either[ErrorADT, AllGameCredentials]]

  def gameEnded(gameId: String, secret: String): ZIO[Any, Nothing, Unit]

}

object MenuGames {
  val live = ZLayer.fromZIO(for {
    _            <- Console.printLine("Initializing MenuGames Service...")
    gamesRef     <- Ref.make(Vector.empty[MenuGameWithPlayers])
    semaphore    <- Semaphore.make(1L)
    crypto       <- ZIO.service[services.crypto.Crypto]
    localstorage <- ZIO.service[services.localstorage.LocalStorage]
    events       <- ZIO.service[services.events.Events]
    storedGames  <- localstorage.retrieveFrom[Vector[MenuGameWithPlayers]](storageKey)
    _            <- gamesRef.set(storedGames.getOrElse(Vector.empty))
    menuGames = BMenuGames(gamesRef, semaphore, crypto, localstorage, events)
  } yield (menuGames: MenuGames))

  private[menugames] inline def storageKey: services.localstorage.LocalStorage.Key = "menu-games"
}
