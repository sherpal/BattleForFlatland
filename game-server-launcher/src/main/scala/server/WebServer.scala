package server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
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
              println("hello")
              val sbtCommand = s"game-server/run -i $gameId -s $gameSecret -h $host"
              sbtProcess.close()
              sbtProcess.stdin.writeLine(sbtCommand)
              complete("ok")
          }
      }
    }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 22223)
}
