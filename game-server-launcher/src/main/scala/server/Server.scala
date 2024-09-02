package server

import io.circe.parser.decode
import menus.data.APIResponse
import menus.data.AllGameCredentials
import errors.ErrorADT
import menus.data.GameCredentialsWithGameInfo

object Server extends cask.MainRoutes {

  override def port: Int = 22223

  @cask.get("/health-check")
  def hello() = "ok"

  @cask.post("/run-game-server")
  def doThing(
      request: cask.Request,
      gameId: String,
      gameSecret: String,
      credentialsUrl: String,
      gameServerReadyUrl: String
  ) = {
    println("""
 _   _                 _____                      
| \ | |               |  __ \                     
|  \| | _____      __ | |  \/ __ _ _ __ ___   ___ 
| . ` |/ _ \ \ /\ / / | | __ / _` | '_ ` _ \ / _ \
| |\  |  __/\ V  V /  | |_\ \ (_| | | | | | |  __/
\_| \_/\___| \_/\_/    \____/\__,_|_| |_| |_|\___|
""")
    println(s"Received request to start $gameId")
    println(s"Requesting all game credentials from server (at $credentialsUrl)...")
    val credentialsBody = requests
      .get(
        credentialsUrl,
        params = Map(
          "gameId" -> gameId,
          "secret" -> gameSecret
        )
      )
      .text()

    (for {
      gameInfo <- decode[APIResponse[GameCredentialsWithGameInfo]](credentialsBody).left
        .map(ErrorADT.fromCirceDecodingError)
      gameInfo <- gameInfo.toEither
    } yield gameInfo) match {
      case Left(err) =>
        val errMessage = s"Error while retrieving game gameInfo: ${err.repr}"
        System.err.println(errMessage)
        cask.Response(errMessage, statusCode = 400)
      case Right(gameInfo) =>
        println(s"Launching game >>>>")
        // todo
        os.proc(
          "java",
          "-jar",
          "game-server/target/scala-3.5.0/game-server.jar",
          gameServerReadyUrl,
          22222, // todo: un-hardcode this in the future...
          GameCredentialsWithGameInfo.encode(gameInfo)
        ).spawn(stdout = os.Inherit)
        cask.Response("ok")
    }
  }

  initialize()

  println(s"Game Server Launcher open at $host:$port in ${os.pwd}")
}
