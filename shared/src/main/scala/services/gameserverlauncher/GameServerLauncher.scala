package services.gameserverlauncher

import errors.ErrorADT.GameServerLauncherCouldNotBeReached
import models.bff.ingame.GameCredentials
import zio.ZIO

trait GameServerLauncher {

  /** Launches the `game-server` with the specified `gameCredentials`. If the game was not able to launch for some
    * reason, should fail with [[GameServerLauncherCouldNotBeReached]].
    *
    * Game ids and secrets are used by the game-server to reach out to the main server. The game-server can be launched
    * using game-server/run -i <id> -s <secret> -h <host>
    *
    * @param gameCredentials
    *   credentials belonging to the game.
    * @return
    *   [[scala.Unit]] if it succeeds.
    */
  def launchGame(gameCredentials: GameCredentials): ZIO[Any, GameServerLauncherCouldNotBeReached.type, Unit]

}
