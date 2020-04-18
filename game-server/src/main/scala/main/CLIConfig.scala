package main

import scopt.OParser

final case class CLIConfig(gameId: String, gameSecret: String, host: String, port: Int)

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
      opt[String]('s', "game-secret")
        .action {
          case (gameSecret, config) => config.copy(gameSecret = gameSecret)
        }
        .text("specify the game secret to use to ask the game server"),
      opt[String]('h', "host")
        .action {
          case (host, config) => config.copy(host = host)
        }
        .text("specify the host on which open the server"),
      opt[Int]('p', "port")
        .action { case (port, config) => config.copy(port = port) }
        .text("specify the port on which open the server"),
      checkConfig { config =>
        if (config.gameId.isEmpty) Left("Game id is missing")
        else if (config.gameSecret.isEmpty) Left("Game secret is missing")
        else Right(())
      }
    )
  }

  def makeConfig(args: List[String]): Option[CLIConfig] =
    OParser.parse(parser, args, CLIConfig("", "", "localhost", 22222))

  class InvalidConfig extends Exception("Configuration options were invalid.")

}
