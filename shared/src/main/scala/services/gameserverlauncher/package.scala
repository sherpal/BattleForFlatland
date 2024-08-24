package services.gameserverlauncher

import errors.ErrorADT.GameServerLauncherCouldNotBeReached
import models.bff.ingame.GameCredentials
import zio.ZIO

def launchGame(
    gameCredentials: GameCredentials
): ZIO[GameServerLauncher, GameServerLauncherCouldNotBeReached.type, Unit] =
  ZIO.serviceWithZIO[GameServerLauncher](_.launchGame(gameCredentials))
