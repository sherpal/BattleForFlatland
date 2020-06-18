package services

import errors.ErrorADT.GameServerLauncherCouldNotBeReached
import models.bff.ingame.GameCredentials
import zio.{Has, ZIO}

package object gameserverlauncher {

  type GameServerLauncher = Has[GameServerLauncher.Service]

  def launchGame(
      gameCredentials: GameCredentials
  ): ZIO[GameServerLauncher, GameServerLauncherCouldNotBeReached.type, Unit] =
    ZIO.accessM(_.get.launchGame(gameCredentials))

}
