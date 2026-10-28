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
import models.bff.ingame.InGameWSProtocol.*
import io.undertow.Undertow
import application.*
import gamelogic.gamestate.GameAction
import gamelogic.utils.IdGeneratorContainer
import concurrent.*
import io.circe.Encoder
import models.bff.ingame.ServerPerformanceSummary
import models.bff.outofgame.gameconfig.PlayerInfo
import communication.udp.{Envelope, JvmUDPSocket, Peer, Rendezvous, TimersImpl}
import communication.udp.Connection.Delivery
import server.udp.{GameServerRendezvous, PlayerSessions}
import io.circe.parser.decode
import menus.data.APIResponse
import errors.ErrorADT
import java.net.{Inet4Address, NetworkInterface}
import scala.concurrent.Await
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

object Server extends cask.MainRoutes {

  private var _allGameInfo: GameCredentialsWithGameInfo = scala.compiletime.uninitialized
  inline def allGameInfo                                = _allGameInfo

  val connectedPlayers                   = AtomicReference(Vector.empty[ConnectedPlayerInfo])
  var actionTranslator: ActionTranslator = scala.compiletime.uninitialized
  var gameMaster: GameMaster             = scala.compiletime.uninitialized

  def initGameManagers(
      connectedPlayers: Vector[ConnectedPlayerInfo]
  ): Unit = {
    println("Everyone is ready, instantiating Game Managers...")
    if allGameInfo == null then {
      throw IllegalStateException("All Game Info must be initialized at this point.")
    }

    actionTranslator = ActionTranslator(connectedPlayers, ActionBuffer[GameAction]())
    given IdGeneratorContainer = IdGeneratorContainer.initialIdGeneratorContainer
    gameMaster = GameMaster(
      actionTranslator,
      allGameInfo.gameInfo,
      connectedPlayers.map(player => player.userName -> player).toMap
    )
  }

  /** The HTTP server only listens on the loopback: the players play through UDP. */
  override def port: Int = 0

  override def host: String = "127.0.0.1"

  override def verbose: Boolean = true

  /** Run by one of the players of the game (see `programs.launchGame` in the menu server), with the
    * arguments `<menu server host> <menu server port> <rendezvous port> <game id> <game secret>
    * [<UDP port>]` (UDP port 0, the default, lets the system choose).
    *
    * The game server gets the game's information from the menu server, starts listening to UDP,
    * registers at the rendezvous (see `communication.udp.Rendezvous`), and tells the menu server
    * that it is ready, and where it can be reached on its local network. It stops by itself when
    * nobody plays anymore (see [[Watchdog]]).
    */
  override def main(args: Array[String]): Unit = {
    println("""
 _____                        _____                           __________________ 
|  __ \                      /  ___|                          | ___ \  ___|  ___|
| |  \/ __ _ _ __ ___   ___  \ `--.  ___ _ ____   _____ _ __  | |_/ / |_  | |_   
| | __ / _` | '_ ` _ \ / _ \  `--. \/ _ \ '__\ \ / / _ \ '__| | ___ \  _| |  _|  
| |_\ \ (_| | | | | | |  __/ /\__/ /  __/ |   \ V /  __/ |    | |_/ / |   | |    
 \____/\__,_|_| |_| |_|\___| \____/ \___|_|    \_/ \___|_|    \____/\_|   \_|    
""")
    if args.length < 5 then {
      System.err.println(
        "Usage: <menu server host> <menu server port> <rendezvous port> <game id> <game secret> [<UDP port>]"
      )
      sys.exit(2)
    }
    val menuServerHost = args(0)
    val menuServerUrl  = s"http://$menuServerHost:${args(1)}"
    val rendezvousPort = args(2).toInt
    val gameId         = args(3)
    val gameSecret     = args(4)
    val udpPort        = args.lift(5).fold(0)(_.toInt)

    println(s"Requesting the game's information from $menuServerUrl...")
    val credentialsBody = requests
      .get(
        s"$menuServerUrl/api/bff/internal/get-all-credentials",
        params = Map("gameId" -> gameId, "secret" -> gameSecret)
      )
      .text()
    _allGameInfo = (for {
      response <- decode[APIResponse[GameCredentialsWithGameInfo]](credentialsBody).left
        .map(ErrorADT.fromCirceDecodingError)
      gameInfo <- response.toEither
    } yield gameInfo).fold(error => throw IllegalStateException(error.repr), identity)

    if (!verbose) cask.main.Main.silenceJboss()
    val server = Undertow.builder
      .addHttpListener(port, host)
      .setHandler(defaultHandler)
      .build
    server.start()
    startUdp(udpPort, Peer(menuServerHost, rendezvousPort), gameSecret)
    Watchdog.start(() => humansConnected.get().nonEmpty)

    val localAddresses = Server.localAddresses
    println(s"Addresses on the local network: ${localAddresses.mkString(", ")}")
    requests.get(
      s"$menuServerUrl/api/bff/internal/game-server-ready",
      params = Map(
        "gameId"    -> allGameInfo.gameInfo.id,
        "secret"    -> allGameInfo.secret,
        "port"      -> playerSessions.localPort.toString,
        "addresses" -> localAddresses.mkString(",")
      )
    )
    println("Server is warned that I'm ready")

  }

  @cask.get("/health-check")
  def healthCheck() = "ok"

  @cask.websocket("/ws/performance-summary-updates")
  def performanceSummaryUpdates() = cask.WsHandler { channel =>
    cask.WsActor { case cask.Ws.Text(_) =>
      if gameMaster != null then
        channel.send(
          cask.Ws.Text(
            Encoder[Option[ServerPerformanceSummary]].apply(gameMaster.performanceSummary).noSpaces
          )
        )
    }
  }

  /** Checks that these credentials are the ones of a player of this game. On failure, gives the
    * http status code and the explanation.
    */
  private def authorize(
      userName: String,
      secret: String,
      gameId: String
  ): Either[(Int, String), PlayerInfo] =
    if allGameInfo == null then Left((429, "nope"))
    else
      for {
        _ <- Either.cond(
          gameId == allGameInfo.gameInfo.id,
          (),
          (400, s"Wrong game id, you sent me $gameId but I want something else")
        )
        userCreds = GameUserCredentials(userName, gameId, secret)
        expectedUserCreds <- allGameInfo.allGameCredentials.allGameUserCredentials
          .find(_.userName == userName)
          .toRight((400, s"User $userName does not belong to this game"))
        _ <- Either.cond(
          userCreds == expectedUserCreds,
          (),
          (400, s"Incorrect secret for $userName")
        )
      } yield allGameInfo.gameInfo.game.gameConfiguration.playersInfo(userName)

  private def playerConnected(playerInfo: PlayerInfo, send: InGameWSProtocol => Unit): Unit = {
    println(s"User is connected: $playerInfo")
    connectedPlayers.updateAndGet(_ :+ ConnectedPlayerInfo(send, playerInfo, isReady = false))
    send(InGameWSProtocol.AllPlayers(allGameInfo.gameInfo.players.map(_.name)))
  }

  /** What to do with a message of a player, whatever the way it reached us. */
  private def handleMessage(
      userName: String,
      message: InGameWSProtocol,
      sendMessage: InGameWSProtocol => Unit
  ): Unit =
    message match
      case GameActionWrapper(gameActions) =>
        // no check that action translator is not null, we accept our fate if that happens
        // a check is one extra computation, and there is no reason for that
        actionTranslator.newGameActions(userName, gameActions)
      case ping: Ping => sendMessage(ping.pong(System.currentTimeMillis()))
      case Ready(userName) =>
        println(s"$userName is ready!")
        val newConnectedPlayers = connectedPlayers.updateAndGet(_.map {
          case info if info.userName == userName => info.copy(isReady = true)
          case info => info
        })
        newConnectedPlayers.foreach(_.send(BuddyIsReady(userName)))
        // every human player of the game, not only the ones connected so far (once)
        val everyoneHere = allGameInfo.gameInfo.players
          .forall(user => newConnectedPlayers.exists(_.userName == user.name))
        if gameMaster == null && everyoneHere && newConnectedPlayers.forall(_.isReady) then {
          initGameManagers(newConnectedPlayers)
        }
      case ReadyToStart(userName) =>
        if gameMaster == null then {
          println(
            s"Received ready to start from $userName before game master is ready, going to ignore..."
          )
        } else gameMaster.newPlayerIsReady(userName)
      case LetsBegin =>
        if gameMaster == null then {
          println(
            "Received LetsBegin but Game Master is null. Going to ignore..."
          )
        } else gameMaster.letsBegin()
      case in: InGameWSProtocol.Incoming =>
        println(s"Received $in, going to ignore...")

  @cask.websocket("/ws/connect")
  def connectToGame(
      ctx: cask.Request,
      userName: String,
      secret: String,
      gameId: String
  ): cask.WebsocketResult =
    authorize(userName, secret, gameId) match {
      case Left((statusCode, message)) => cask.Response(message, statusCode = statusCode)
      case Right(playerInfo) =>
        import BFFPicklers.*
        cask.WsHandler { channel =>
          def sendMessage(message: InGameWSProtocol): Unit =
            channel.send(cask.Ws.Binary(Pickle.intoBytes(message).array()))
          playerConnected(playerInfo, sendMessage)
          cask.WsActor {
            case cask.Ws.Binary(bytes) =>
              handleMessage(
                userName,
                Unpickle[InGameWSProtocol].fromBytes(ByteBuffer.wrap(bytes)),
                sendMessage
              )
            case cask.Ws.Text(data) =>
              println(s"I don't handle text data")
          }
        }
    }

  // UDP: the same game, for the clients that cannot speak websockets.

  private var playerSessions: PlayerSessions = scala.compiletime.uninitialized

  /** How the messages to the players are sent through UDP. Everything must arrive, in order, except
    * what is about the connection itself (which the connection now handles on its own).
    */
  private def udpDelivery(message: InGameWSProtocol): Delivery = message match {
    case _: Pong | HeartBeat => Delivery.Normal
    case _ => Delivery.OrderedReliable
  }

  /** The players connected (through UDP), for the [[Watchdog]]. */
  private val humansConnected = AtomicReference(Set.empty[String])

  private def startUdp(udpPort: Int, rendezvousAddress: Peer, gameSecret: String): Unit = {
    import BFFPicklers.*
    val socket = JvmUDPSocket()
    val timers = TimersImpl()
    val rendezvous = GameServerRendezvous(
      socket,
      timers,
      rendezvousAddress,
      allGameInfo.gameInfo.id,
      gameSecret,
      userName => playerSessions.isConnected(userName)
    )
    playerSessions = PlayerSessions(
      Rendezvous.SharedSocket(socket, rendezvous.received, rendezvous.receivedStun),
      timers,
      authenticate = credentials =>
        authorize(credentials.userName, credentials.userSecret, credentials.gameId).fold(
          { case (statusCode, _) =>
            Left(
              if statusCode == 429 then Envelope.RejectReason.NotReady
              else Envelope.RejectReason.Unauthorized
            )
          },
          _ => Right(())
        ),
      listener = new PlayerSessions.Listener {
        def connected(userName: String): Unit = {
          humansConnected.updateAndGet(_ + userName)
          def send(message: InGameWSProtocol): Unit =
            playerSessions.send(userName, message, udpDelivery(message))
          playerConnected(allGameInfo.gameInfo.game.gameConfiguration.playersInfo(userName), send)
        }

        def message(userName: String, message: InGameWSProtocol): Unit =
          handleMessage(
            userName,
            message,
            reply => playerSessions.send(userName, reply, udpDelivery(reply))
          )

        def lost(userName: String): Unit = {
          println(s"Lost UDP connection with $userName")
          humansConnected.updateAndGet(_ - userName)
          // once the game is set up, the players are known to the game; they may come back
          if gameMaster == null then
            connectedPlayers.updateAndGet(_.filterNot(_.userName == userName))
          ()
        }
      }
    )
    playerSessions.start(udpPort)
    println(s"Listening to UDP on ${playerSessions.localPort}")

    rendezvous.start()
    // Without the rendezvous, only the players on our local network can join
    Try(Await.result(rendezvous.isRegistered, 5.seconds)).failed.foreach(_ =>
      println(
        s"The rendezvous ($rendezvousAddress) did not answer: only the players on the local network may join."
      )
    )
  }

  /** Our IPv4 addresses on the local networks (the players' clients only speak IPv4). */
  def localAddresses: Vector[String] =
    NetworkInterface.getNetworkInterfaces.asScala.toVector
      .filter(interface => interface.isUp && !interface.isLoopback)
      .flatMap(_.getInetAddresses.asScala)
      .collect { case address: Inet4Address => address.getHostAddress }

  initialize()

}
