package server

import typings.node.childProcessMod.{spawn, ChildProcessWithoutNullStreams}
import typings.express.{mod => expressMod}
import typings.expressServeStaticCore.mod._
import typings.node.global.console
import typings.node.processMod
import typings.expressServeStaticCore.mod.ParamsDictionary
import typings.qs.mod.ParsedQs

import scala.scalajs.js

object Main {

  val child: ChildProcessWithoutNullStreams = spawn("sbt")
  child.stdin_ChildProcessWithoutNullStreams.setDefaultEncoding("utf-8")
  child.stdout_ChildProcessWithoutNullStreams.pipe(typings.node.global.process.stdout)

  private def runSBTCommand(command: String): Unit = child.stdin_ChildProcessWithoutNullStreams.write(
    command + "\n"
  )

  object WelcomeController {

    val Router: Router =
      expressMod.Router()

    trait LaunchServerData extends js.Object {
      val gameId: String
      val gameSecret: String
      val host: String
    }

    val launchServerService: RequestHandler[ParamsDictionary, String, Unit, LaunchServerData] =
      (req, res, _) => {
        val gameId     = req.query.gameId
        val gameSecret = req.query.gameSecret
        val host       = req.query.host

        val sbtCommand = s"game-server/run -i $gameId -s $gameSecret -h $host"
        runSBTCommand(sbtCommand)

        res.send("ok")
      }

    Router
      .get[ParamsDictionary, String, Unit, LaunchServerData]("/", launchServerService)
  }

  def main(args: Array[String]): Unit = {
    val app  = expressMod.^()
    val port = 22223 // processMod.^.env.get("PORT").flatMap(_.toOption).fold(30000)(_.toInt)
    app.use[ParamsDictionary, js.Any, js.Any, ParsedQs]("/run-game-server", WelcomeController.Router)

    app.listen(port, () => console.log(s"Listening at http://localhost:$port/"))
  }

}
