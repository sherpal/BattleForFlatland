package server.udp

import communication.BFFPicklers.*
import communication.udp.*
import communication.udp.Envelope.{Nonce, Packet}
import communication.udp.Connection.Delivery
import models.bff.ingame.InGameWSProtocol

import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}

/** The whole stack on the loopback interface, with real sockets and timers. */
final class PlayerSessionsLoopbackTest extends munit.FunSuite {

  private def await[A](queue: LinkedBlockingQueue[A]): A = {
    val result = queue.poll(5, TimeUnit.SECONDS)
    assert(result != null, "nothing arrived in time")
    result
  }

  test(
    "a player proves who they are, then exchanges messages with the game, and agrees on the time"
  ) {
    val serverMessages = new LinkedBlockingQueue[(String, InGameWSProtocol)]()
    val events         = new LinkedBlockingQueue[String]()
    val sessions = new PlayerSessions(
      new JvmUDPSocket,
      new TimersImpl,
      (userName, _) =>
        Either.cond(userName == "alice", "secret", Envelope.RejectReason.Unauthorized),
      new PlayerSessions.Listener {
        def connected(userName: String): Unit = events.put(s"connected $userName")
        def message(userName: String, message: InGameWSProtocol): Unit =
          serverMessages.put((userName, message))
        def lost(userName: String): Unit = events.put(s"lost $userName")
      }
    )
    sessions.start(0, Some("127.0.0.1"))
    // the port was chosen by the system
    val serverPeer = Peer("127.0.0.1", sessions.localPort)

    val packets      = new LinkedBlockingQueue[Packet]()
    val clientSocket = new JvmUDPSocket
    val fromServer   = new LinkedBlockingQueue[InGameWSProtocol]()
    @volatile var connection: Connection[InGameWSProtocol] = null
    @volatile var keys: Envelope.SessionKeys               = null
    clientSocket.setMessageCallback { (bytes, _) =>
      Envelope.parse(bytes).foreach {
        case data: Packet.Data => keys.open(data).foreach(connection.onMessage)
        case other => packets.put(other)
      }
    }
    clientSocket.bind(0, Some("127.0.0.1"))
    def send(packet: Packet) =
      clientSocket.send(Envelope.encode(packet), serverPeer.port, serverPeer.address)

    send(Packet.Hello("alice", 1, Nonce.fresh(() => 1)))
    val challenge = await(packets).asInstanceOf[Packet.Challenge]

    // wrong secret
    send(Envelope.proof("alice", 1, challenge, Hmac("nope")))
    assertEquals(await(packets), Packet.Rejected(Envelope.RejectReason.Unauthorized))

    // right secret
    keys = Envelope.SessionKeys(Hmac("secret"), challenge, towardsServer = true)
    connection = new Connection[InGameWSProtocol](
      serverPeer,
      new Envelope.DataSocket(clientSocket, keys),
      new TimersImpl,
      fromServer.put(_)
    )
    send(Envelope.proof("alice", 1, challenge, Hmac("secret")))
    assert(keys.isAccepted(await(packets).asInstanceOf[Packet.Accepted]))
    assertEquals(await(events), "connected alice")

    connection.send(InGameWSProtocol.Ready("alice"), Delivery.OrderedReliable)
    assertEquals(await(serverMessages), ("alice", InGameWSProtocol.Ready("alice")))

    sessions.send("alice", InGameWSProtocol.EveryoneIsReady, Delivery.OrderedReliable)
    assertEquals(await(fromServer), InGameWSProtocol.EveryoneIsReady)

    val delta = new LinkedBlockingQueue[Long]()
    connection.computeLinkTime(sampleTime = 10, sampleNumber = 5, delta.put(_))
    val offset = await(delta)
    assert(math.abs(offset) < 100, s"same machine, same clock, but offset is $offset")

    sessions.close()
    clientSocket.close()
  }
}
