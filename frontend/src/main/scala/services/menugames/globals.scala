package services.menugames

import zio.*
import models.bff.outofgame.MenuGameWithPlayers
import errors.ErrorADT
import models.bff.outofgame.gameconfig.PlayerInfo
import models.bff.outofgame.gameconfig.GameConfiguration

def menuGames: ZIO[MenuGames, Nothing, Vector[MenuGameWithPlayers]] =
  ZIO.serviceWithZIO[MenuGames](_.menuGames)

def launchGame(gameId: String): ZIO[MenuGames, Nothing, Either[ErrorADT, Boolean]] =
  ZIO.serviceWithZIO[MenuGames](_.launchGame(gameId))

def createGame(
    gameName: String
): ZIO[MenuGames, Nothing, Either[ErrorADT, MenuGameWithPlayers]] =
  ZIO.serviceWithZIO[MenuGames](_.createGame(gameName))

def joinGame(
    gameId: String
): ZIO[MenuGames, Nothing, Either[ErrorADT, Vector[MenuGameWithPlayers]]] =
  ZIO.serviceWithZIO[MenuGames](_.joinGame(gameId))

def leaveGame(gameId: String): ZIO[MenuGames, Nothing, Either[ErrorADT, MenuGameWithPlayers]] =
  ZIO.serviceWithZIO[MenuGames](_.leaveGame(gameId))

def kickPlayer(
    gameId: String,
    playerName: String
): ZIO[MenuGames, Nothing, Either[ErrorADT, MenuGameWithPlayers]] =
  ZIO.serviceWithZIO[MenuGames](_.kickPlayer(gameId, playerName))

def changePlayerInfo(
    gameId: String,
    playerInfo: PlayerInfo
): ZIO[MenuGames, Nothing, Either[ErrorADT, MenuGameWithPlayers]] =
  ZIO.serviceWithZIO[MenuGames](_.changePlayerInfo(gameId, playerInfo))

def changeGameConfig(
    gameId: String,
    gameConfigMetadata: GameConfiguration.GameConfigMetadata
): ZIO[MenuGames, Nothing, Either[ErrorADT, MenuGameWithPlayers]] =
  ZIO.serviceWithZIO[MenuGames](_.changeGameConfig(gameId, gameConfigMetadata))

def gameInfo(gameId: String) =
  menuGames.map(_.find(_.id == gameId).toRight(ErrorADT.GameDoesNotExist(gameId))).absolve
