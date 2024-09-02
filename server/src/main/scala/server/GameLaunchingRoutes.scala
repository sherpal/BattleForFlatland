package server

import ziocask.WithZIOEndpoints
import menus.data.GameCredentials
import menus.data.APIResponse
import services.{events, menugames}
import zio.*
import menus.data.AllGameCredentials
import models.bff.outofgame.MenuGameWithPlayers
import menus.data.GameCredentialsWithGameInfo

class GameLaunchingRoutes()(using
    val runtime: zio.Runtime[BackendEnv],
    ac: castor.Context,
    log: cask.util.Logger
) extends cask.Routes
    with WithZIOEndpoints[BackendEnv] {

  @caskz.getJ[APIResponse[GameCredentialsWithGameInfo]]("api/bff/internal/get-all-credentials")
  def retrieveAllGameCredentials(gameId: String, secret: String) = (for {
    _           <- Console.printLine(s"Request to provide credentials for $gameId").ignore
    credentials <- menugames.retrieveAllGameCredentials(gameId, secret)
  } yield credentials).map(APIResponse.fromEither)

  @caskz.getJ[APIResponse[Boolean]]("api/bff/internal/game-server-ready")
  def gameServerReady(ctx: cask.Request, gameId: String, secret: String, port: Int) = (for {
    gameInfo <- menugames.retrieveAllGameCredentials(gameId, secret).flatMap(ZIO.fromEither)
    _ <- events.dispatchEvent(events.Event.GameCredentials(gameInfo.allGameCredentials, port))
  } yield true).either.map(APIResponse.fromEither)

  initialize()
}
