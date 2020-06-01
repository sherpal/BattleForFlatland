// Run "amm game-server-launcher.sc" to spawn this server launcher.
// To start a game server, send a get request to localhost/22223 with query parameters
// - gameId (id of the game to launch)
// - gameSecret (game secret of the game to launch)
// This script can be used in development to accelarate and easen dev cycles.
import $ivy.`com.typesafe.akka::akka-actor-typed:2.6.4`
import $ivy.`com.typesafe.akka::akka-stream-typed:2.6.4`
import $ivy.`com.typesafe.akka::akka-http:10.1.12`
import $ivy.`com.lihaoyi::os-lib:0.7.0` 

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import scala.io.StdIn
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object WebServer {
  val sbtProcess = os.proc("sbt").spawn(stdout = os.Inherit, stderr = os.Inherit)

  implicit val system = ActorSystem("my-system")
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  val route =
    path("run-game-server") {
      get {
          parameters(Symbol("gameId"), Symbol("gameSecret"), Symbol("host")) { (gameId, gameSecret, host) =>
              val sbtCommand = s"game-server/run -i $gameId -s $gameSecret -h $host"
              sbtProcess.stdin.writeLine(sbtCommand)
              complete("ok")
          }
      }
    }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 22223)
}

WebServer

println(s"Server online at http://localhost:22223/\nPress RETURN to stop...")
StdIn.readLine() // let it run until user presses return
WebServer.bindingFuture
  .flatMap(_.unbind()) // trigger unbinding from the port
  .onComplete(_ => WebServer.system.terminate()) // and shutdown when done
