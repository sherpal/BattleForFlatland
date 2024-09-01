package server

import ziocask.WithZIOEndpoints
import menus.data.GameCredentials
import menus.data.APIResponse
import services.menugames
import zio.*
import menus.data.AllGameCredentials

class GameLaunchingRoutes()(using
    val runtime: zio.Runtime[BackendEnv],
    ac: castor.Context,
    log: cask.util.Logger
) extends cask.Routes
    with WithZIOEndpoints[BackendEnv] {

  @caskz.getJ[APIResponse[AllGameCredentials]]("api/bff/internal/get-all-credentials")
  def retrieveAllGameCredentials(gameId: String, secret: String) = (for {
    _           <- Console.printLine(s"Request to provide credentials for $gameId").ignore
    credentials <- menugames.retrieveAllGameCredentials(gameId, secret)
  } yield credentials).map(APIResponse.fromEither)

  initialize()
}
