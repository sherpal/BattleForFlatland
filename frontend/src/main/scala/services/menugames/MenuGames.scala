package services.menugames

import zio.*
import models.bff.outofgame.MenuGameWithPlayers
import errors.ErrorADT
import models.bff.outofgame.gameconfig.PlayerInfo
import models.bff.outofgame.gameconfig.GameConfiguration
import models.bff.outofgame.PlayerClasses

trait MenuGames {

  def menuGames: ZIO[Any, Nothing, Vector[MenuGameWithPlayers]]

  def launchGame(gameId: String): ZIO[Any, Nothing, Either[ErrorADT, Boolean]]

  def createGame(
      gameName: String
  ): ZIO[Any, Nothing, Either[ErrorADT, MenuGameWithPlayers]]

  def joinGame(
      gameId: String
  ): ZIO[Any, Nothing, Either[ErrorADT, Vector[MenuGameWithPlayers]]]

  def leaveGame(gameId: String): ZIO[Any, Nothing, Either[ErrorADT, MenuGameWithPlayers]]

  def kickPlayer(
      gameId: String,
      playerName: String
  ): ZIO[Any, Nothing, Either[ErrorADT, MenuGameWithPlayers]]

  def changePlayerInfo(
      gameId: String,
      playerInfo: PlayerInfo
  ): ZIO[Any, Nothing, Either[ErrorADT, MenuGameWithPlayers]]

  def addAIToGame(gameId: String): ZIO[Any, Nothing, Either[ErrorADT, Boolean]]

  def removeAIFromGame(
      gameId: String,
      cls: PlayerClasses
  ): ZIO[Any, Nothing, Either[ErrorADT, Boolean]]

  def changeGameConfig(
      gameId: String,
      gameConfigMetadata: GameConfiguration.GameConfigMetadata
  ): ZIO[Any, Nothing, Either[ErrorADT, MenuGameWithPlayers]]

}

object MenuGames {

  val live = ZLayer.fromZIO(for {
    _          <- Console.printLine("Initializing MenuGames service...").orDie
    http       <- ZIO.service[services.http.HttpClient]
    fMenuGames <- ZIO.succeed(FMenuGames(http))
  } yield (fMenuGames: MenuGames))

}
