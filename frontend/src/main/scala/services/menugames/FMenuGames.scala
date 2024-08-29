package services.menugames

import errors.ErrorADT

import models.bff.outofgame.MenuGameWithPlayers

import zio.ZIO
import menus.data.APIResponse
import menus.data.CreateGameFormData
import menus.data.JoinGameFormData
import models.bff.outofgame.gameconfig.PlayerInfo
import menus.data.ChangePlayerInfoFormData
import models.bff.outofgame.gameconfig.GameConfiguration
import menus.data.ChangeGameMetadataFormData
import menus.data.GameIdFormData
import menus.data.KickPlayerFormData

private[menugames] class FMenuGames(http: services.http.HttpClient) extends MenuGames {

  override def menuGames: ZIO[Any, Nothing, Vector[MenuGameWithPlayers]] =
    http.get[Vector[MenuGameWithPlayers]](models.bff.Routes.allGames).orDie

  override def createGame(
      gameName: String
  ): ZIO[Any, Nothing, Either[ErrorADT, MenuGameWithPlayers]] =
    http
      .post[APIResponse[MenuGameWithPlayers]](
        models.bff.Routes.newMenuGame,
        CreateGameFormData(gameName)
      )
      .map(_.toEither)
      .orDie

  override def joinGame(
      gameId: String
  ): ZIO[Any, Nothing, Either[ErrorADT, Vector[MenuGameWithPlayers]]] =
    http
      .post[APIResponse[Vector[MenuGameWithPlayers]]](
        models.bff.Routes.joinGame,
        JoinGameFormData(gameId, None)
      )
      .orDie
      .map(_.toEither)

  override def leaveGame(gameId: String): ZIO[Any, Nothing, Either[ErrorADT, MenuGameWithPlayers]] =
    http
      .post[APIResponse[MenuGameWithPlayers]](
        models.bff.Routes.leaveGame,
        GameIdFormData(gameId)
      )
      .orDie
      .map(_.toEither)

  override def kickPlayer(
      gameId: String,
      playerName: String
  ): ZIO[Any, Nothing, Either[ErrorADT, MenuGameWithPlayers]] =
    http
      .post[APIResponse[MenuGameWithPlayers]](
        models.bff.Routes.kickPlayer,
        KickPlayerFormData(gameId, playerName)
      )
      .orDie
      .map(_.toEither)

  override def changePlayerInfo(
      gameId: String,
      playerInfo: PlayerInfo
  ): ZIO[Any, Nothing, Either[ErrorADT, MenuGameWithPlayers]] =
    http
      .post[APIResponse[MenuGameWithPlayers]](
        models.bff.Routes.changePlayerInfo,
        ChangePlayerInfoFormData(gameId, playerInfo)
      )
      .orDie
      .map(_.toEither)

  override def changeGameConfig(
      gameId: String,
      gameConfigMetadata: GameConfiguration.GameConfigMetadata
  ): ZIO[Any, Nothing, Either[ErrorADT, MenuGameWithPlayers]] =
    http
      .post[APIResponse[MenuGameWithPlayers]](
        models.bff.Routes.changeGameConfig,
        ChangeGameMetadataFormData(gameId, gameConfigMetadata)
      )
      .orDie
      .map(_.toEither)

}
