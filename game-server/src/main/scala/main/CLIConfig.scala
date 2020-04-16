package main

import scopt.OParser

final case class CLIConfig(gameId: String)

object CLIConfig {
  private val builder = OParser.builder[CLIConfig]

  private val parser = {
    import builder._

    OParser.sequence(
      programName("Battle for Flatland game server"),
      opt[String]('i', "game-id")
//        .validate {
//          case "" => Left("Missing game id")
//          case _  => Right(())
//        }
        .action {
          case (gameId, config) => config.copy(gameId = gameId)
        }
        .text("specify the game id to fetch information from"),
      checkConfig { config =>
        if (config.gameId.isEmpty) Left("Game id is missing")
        else Right(())
      }
    )
  }

  def makeConfig(args: List[String]): Option[CLIConfig] =
    OParser.parse(parser, args, CLIConfig(""))

  class InvalidConfig extends Exception("Configuration options were invalid.")

}
