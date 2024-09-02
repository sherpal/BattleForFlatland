package server

import scala.util.Try
import menus.data.GameCredentialsWithGameInfo
import java.util.concurrent.atomic.AtomicReference
import application.ConnectedPlayerInfo
import models.bff.ingame.GameUserCredentials
import communication.BFFPicklers
import boopickle.Default.*
import models.bff.ingame.InGameWSProtocol
import java.nio.ByteBuffer
import models.bff.ingame.InGameWSProtocol.HeartBeat
import models.bff.ingame.InGameWSProtocol.Ping
import models.bff.ingame.InGameWSProtocol.Pong
import models.bff.ingame.InGameWSProtocol.Ready
import models.bff.ingame.InGameWSProtocol.ReadyToStart
import models.bff.ingame.InGameWSProtocol.LetsBegin
import models.bff.ingame.InGameWSProtocol.GameActionWrapper
import models.bff.ingame.InGameWSProtocol.RemoveActions
import models.bff.ingame.InGameWSProtocol.AddAndRemoveActions
import models.bff.ingame.InGameWSProtocol.YourEntityIdIs
import models.bff.ingame.InGameWSProtocol.StartingBossPosition
import io.undertow.Undertow

object Server extends cask.MainRoutes {

  private var _runOnPort: Int = scala.compiletime.uninitialized

  private var _allGameInfo: GameCredentialsWithGameInfo = scala.compiletime.uninitialized
  inline def allGameInfo                                = _allGameInfo

  val connectedPlayers = AtomicReference(Vector.empty[ConnectedPlayerInfo])

  override def port: Int = _runOnPort

  override def host: String = "0.0.0.0"

  override def verbose: Boolean = true

  override def main(args: Array[String]): Unit = {
    println("""
 _____                        _____                           __________________ 
|  __ \                      /  ___|                          | ___ \  ___|  ___|
| |  \/ __ _ _ __ ___   ___  \ `--.  ___ _ ____   _____ _ __  | |_/ / |_  | |_   
| | __ / _` | '_ ` _ \ / _ \  `--. \/ _ \ '__\ \ / / _ \ '__| | ___ \  _| |  _|  
| |_\ \ (_| | | | | | |  __/ /\__/ /  __/ |   \ V /  __/ |    | |_/ / |   | |    
 \____/\__,_|_| |_| |_|\___| \____/ \___|_|    \_/ \___|_|    \____/\_|   \_|    
""")
    val gameServerReadyUrl = args(0)
    _runOnPort = args(1).toInt
    _allGameInfo = Try(args(2)).flatMap(data => GameCredentialsWithGameInfo.decode(data).toTry).get

    if (!verbose) cask.main.Main.silenceJboss()
    val server = Undertow.builder
      .addHttpListener(port, host)
      .setHandler(defaultHandler)
      .build
    server.start()
    println(s"Listening to $host:$port")
    requests.get(
      gameServerReadyUrl,
      params = Map(
        "gameId" -> allGameInfo.gameInfo.id,
        "secret" -> allGameInfo.secret,
        "port"   -> port.toString
      )
    )
    println("Server is warned that I'm ready")

  }

  @cask.get("/health-check")
  def healthCheck() = "ok"

  @cask.websocket("/ws/connect")
  def connectToGame(
      ctx: cask.Request,
      userName: String,
      secret: String,
      gameId: String
  ): cask.WebsocketResult =
    if allGameInfo == null then cask.Response("nope", statusCode = 429)
    else {
      (for {
        _ <- Either.cond(
          gameId == allGameInfo.gameInfo.id,
          (),
          cask.Response(
            s"Wrong game id, you sent me $gameId but I want something else",
            statusCode = 400
          )
        )
        userCreds = GameUserCredentials(userName, gameId, secret)
        expectedUserCreds <- allGameInfo.allGameCredentials.allGameUserCredentials
          .find(_.userName == userName)
          .toRight(cask.Response(s"User $userName does not belong to this game", statusCode = 400))
        _ <- Either.cond(
          userCreds == expectedUserCreds,
          (),
          cask.Response(s"Incorrect secret for $userName", statusCode = 400)
        )
        playerInfo = allGameInfo.gameInfo.game.gameConfiguration.playersInfo(userName)
      } yield playerInfo) match {
        case Left(response) => response
        case Right(playerInfo) =>
          println(s"User is connected: $playerInfo")
          import BFFPicklers.*
          cask.WsHandler { channel =>
            def sendMessage(message: InGameWSProtocol): Unit =
              channel.send(cask.Ws.Binary(Pickle.intoBytes(message).array()))
            connectedPlayers.updateAndGet(
              _ :+ ConnectedPlayerInfo(sendMessage, playerInfo, isReady = false)
            )
            cask.WsActor {
              case cask.Ws.Binary(bytes) =>
                val message = Unpickle[InGameWSProtocol].fromBytes(ByteBuffer.wrap(bytes))
                // todo
                message match
                  case GameActionWrapper(gameActions) => ??? // todo
                  case ping: Ping =>
                    println(ping)
                    sendMessage(ping.pong(System.currentTimeMillis()))
                  case Ready(userName) =>
                    println(s"$userName is ready!")
                    val newConnectedPlayers = connectedPlayers.updateAndGet(_.map {
                      case info if info.userName == userName => info.copy(isReady = true)
                      case info                              => info
                    })
                    if newConnectedPlayers.forall(_.isReady) then {
                      println("Everyone is ready, need to go to next part of the plan...")
                    }
                    ??? // todo
                  case ReadyToStart(userId) => ??? // todo
                  case LetsBegin            => ??? // todo
                  case in: InGameWSProtocol.Incoming =>
                    println(s"Received $in, going to ignore...")
              case cask.Ws.Text(data) =>
                println(s"I don't handle text data")
            }
          }
      }
    }

  initialize()

}
