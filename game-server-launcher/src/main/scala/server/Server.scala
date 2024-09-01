package server

import io.circe.parser.decode
import menus.data.APIResponse
import menus.data.AllGameCredentials
import errors.ErrorADT

object Server extends cask.MainRoutes {

  override def port: Int = 22223

  @cask.get("/health-check")
  def hello() = "ok"

  @cask.post("/run-game-server")
  def doThing(request: cask.Request, gameId: String, gameSecret: String, credentialsUrl: String) = {
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
      decodedCredentials <- decode[APIResponse[AllGameCredentials]](credentialsBody).left
        .map(ErrorADT.fromCirceDecodingError)
      credentials <- decodedCredentials.toEither
    } yield credentials) match {
      case Left(err) =>
        val errMessage = s"Error while retrieving game credentials: ${err.repr}"
        System.err.println(errMessage)
        cask.Response(errMessage, statusCode = 400)
      case Right(credentials) =>
        println("I retrieved the credentials!")
        println(credentials)

        println(s"Launching game $gameId with secret $gameSecret")
        // todo
        os.proc("python", "main.py").spawn(stdout = os.Inherit)
        cask.Response("ok")
    }
  }

  initialize()

  println(s"Game Server Launcher open at $host:$port in ${os.pwd}")
}
