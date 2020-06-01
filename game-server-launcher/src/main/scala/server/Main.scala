package server

import scala.io.StdIn

object Main {

  def main(args: Array[String]): Unit = {
    WebServer

    import WebServer.executionContext

    println(s"Server online at http://localhost:22223/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    WebServer.bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => WebServer.system.terminate()) // and shutdown when done

  }

}
