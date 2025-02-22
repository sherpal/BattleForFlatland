package services.menugames

import zio.*
import models.bff.outofgame.MenuGameWithPlayers
import menus.data.User
import errors.ErrorADT
import models.bff.outofgame.gameconfig.PlayerInfo
import models.bff.outofgame.gameconfig.GameConfiguration
import menus.data.AllGameCredentials
import menus.data.GameCredentialsWithGameInfo
import models.bff.outofgame.PlayerClasses

def menuGames: ZIO[MenuGames, Nothing, Vector[MenuGameWithPlayers]] =
  ZIO.serviceWithZIO[MenuGames](_.menuGames)

def createGame(
    gameName: String,
    maybeHashedPassword: Option[String],
    gameCreator: User
): ZIO[MenuGames, Nothing, Either[ErrorADT, MenuGameWithPlayers]] =
  ZIO.serviceWithZIO[MenuGames](_.createGame(gameName, maybeHashedPassword, gameCreator))

def deleteGame(gameId: String): ZIO[MenuGames, Nothing, Unit] =
  ZIO.serviceWithZIO[MenuGames](_.deleteGame(gameId))

def joinGame(
    user: User,
    gameId: String,
    maybePassword: Option[String]
): ZIO[MenuGames, Nothing, Either[ErrorADT, Vector[MenuGameWithPlayers]]] =
  ZIO.serviceWithZIO[MenuGames](_.joinGame(user, gameId, maybePassword))

def addAIToGame(gameId: String): ZIO[MenuGames, Nothing, Either[ErrorADT, Unit]] =
  ZIO.serviceWithZIO[MenuGames](_.addAIToGame(gameId))

def removeAIFromGame(
    gameId: String,
    cls: PlayerClasses
): ZIO[MenuGames, Nothing, Either[ErrorADT, Unit]] =
  ZIO.serviceWithZIO[MenuGames](_.removeAIFromGame(gameId, cls))

def removePlayer(
    requester: User,
    gameId: String
): ZIO[MenuGames, Nothing, Either[ErrorADT, MenuGameWithPlayers]] =
  ZIO.serviceWithZIO[MenuGames](_.removePlayer(requester, gameId))

def changePlayerInfo(
    user: User,
    gameId: String,
    newPlayerInfo: PlayerInfo
): ZIO[MenuGames, Nothing, Either[ErrorADT, MenuGameWithPlayers]] =
  ZIO.serviceWithZIO[MenuGames](_.changePlayerInfo(user, gameId, newPlayerInfo))

def changeGameMetadata(
    requester: User,
    gameId: String,
    gameMetadata: GameConfiguration.GameConfigMetadata
): ZIO[MenuGames, Nothing, Either[ErrorADT, MenuGameWithPlayers]] =
  ZIO.serviceWithZIO[MenuGames](_.changeGameMetadata(requester, gameId, gameMetadata))

def launchGame(
    requester: User,
    gameId: String
): ZIO[MenuGames, Nothing, Either[ErrorADT, Unit]] =
  ZIO.serviceWithZIO[MenuGames](_.launchGame(requester, gameId))

def retrieveAllGameCredentials(
    gameId: String,
    secret: String
): ZIO[MenuGames, Nothing, Either[ErrorADT, GameCredentialsWithGameInfo]] =
  ZIO.serviceWithZIO[MenuGames](_.retrieveAllGameCredentials(gameId, secret))
