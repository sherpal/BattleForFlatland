package server.udp

import communication.BFFPicklers.*
import communication.udp.*
import communication.udp.Envelope.{Packet, RejectReason}
import communication.udp.Connection.Delivery
import models.bff.ingame.{GameUserCredentials, InGameWSProtocol}
import scala.collection.mutable

final class PlayerSessionsTest extends munit.FunSuite {

  /** Timers whose time only moves when told to. */
  private final class ManualTimers extends Timers {
    private final class Task(var due: Long, val period: Option[Int], val run: () => Unit) {
      var cancelled = false
    }
    private var time  = 0L
    private val tasks = mutable.Buffer.empty[Task]

    override def now(): Long = time

    private def add(delay: Int, period: Option[Int], callback: => Unit): Timers.TimerHandler = {
      val task = new Task(time + delay, period, () => callback)
      tasks += task
      Timers.TimerHandler(() => task.cancelled = true)
    }

    def setTimeout(delay: Int)(callback: => Unit): Timers.TimerHandler = add(delay, None, callback)
    def setInterval(delay: Int)(callback: => Unit): Timers.TimerHandler =
      add(delay, Some(delay), callback)

    def advance(ms: Int): Unit = {
      val target = time + ms
      var next   = tasks.filter(!_.cancelled).minByOption(_.due).filter(_.due <= target)
      while next.isDefined do {
        val task = next.get
        time = task.due
        task.period match {
          case Some(period) => task.due += period
          case None => tasks -= task
        }
        task.run()
        tasks --= tasks.filter(_.cancelled).toList
        next = tasks.minByOption(_.due).filter(_.due <= target)
      }
      time = target
    }
  }

  /** The network is instantaneous, and the server is the only one at the other end. */
  private final class FakeNetwork {
    val serverPeer = Peer("server", 1)
    val clients    = mutable.Map.empty[Peer, FakeClient]
    val serverSocket: UDPSocket = new UDPSocket {
      def peer: Peer                                     = serverPeer
      def bind(port: Int, address: Option[String]): Unit = ()
      def send(message: Array[Byte], port: Int, address: String): Unit =
        clients.get(Peer(address, port)).foreach(_.receive(message.clone()))
      def close(): Unit = closed = true
    }
    var closed = false

    def deliverToServer(bytes: Array[Byte], from: Peer): Unit =
      serverSocket.onMessage(bytes.clone(), from)
  }

  private final class FakeClient(network: FakeNetwork, val peer: Peer, timers: Timers) {
    network.clients(peer) = this

    val packets  = mutable.Buffer.empty[Packet]
    val messages = mutable.Buffer.empty[InGameWSProtocol]

    private val rawSocket: UDPSocket = new UDPSocket {
      def peer: Peer                                     = FakeClient.this.peer
      def bind(port: Int, address: Option[String]): Unit = ()
      def send(message: Array[Byte], port: Int, address: String): Unit =
        network.deliverToServer(message, FakeClient.this.peer)
      def close(): Unit = ()
    }

    /** The connection a client sets up once it was accepted. */
    val connection = new Connection[InGameWSProtocol](
      network.serverPeer,
      new Envelope.DataSocket(rawSocket),
      timers,
      messages += _
    )

    /** An offline client does not answer anything, not even the pings. */
    var online = true

    def receive(datagram: Array[Byte]): Unit = if online then
      Envelope.parse(datagram).foreach { packet =>
        packets += packet
        packet match {
          case Packet.Data(payload) => connection.onMessage(payload)
          case _ => ()
        }
      }

    def sendRaw(datagram: Array[Byte]): Unit = network.deliverToServer(datagram, peer)

    def hello(userName: String, secret: String, gameId: String = "g"): Unit =
      sendRaw(Envelope.hello(GameUserCredentials(userName, gameId, secret)))

    def lastPacket: Option[Packet] = packets.lastOption
  }

  private final class Fixture {
    val timers         = new ManualTimers
    val network        = new FakeNetwork
    val events         = mutable.Buffer.empty[String]
    val serverMessages = mutable.Buffer.empty[(String, InGameWSProtocol)]
    var ready          = true

    private val secrets = Map("alice" -> "a-secret", "bob" -> "b-secret")

    val sessions = new PlayerSessions(
      network.serverSocket,
      timers,
      credentials =>
        if !ready then Left(RejectReason.NotReady)
        else if credentials.gameId == "g" && secrets
            .get(credentials.userName)
            .contains(credentials.userSecret)
        then Right(())
        else Left(RejectReason.Unauthorized),
      new PlayerSessions.Listener {
        def connected(userName: String): Unit = events += s"connected $userName"
        def message(userName: String, message: InGameWSProtocol): Unit =
          serverMessages += ((userName, message))
        def lost(userName: String): Unit = events += s"lost $userName"
      }
    )
    sessions.start(22222)

    def client(name: String, port: Int): FakeClient =
      new FakeClient(network, Peer(name, port), timers)
  }

  test("a player with the right credentials is accepted") {
    val f     = new Fixture
    val alice = f.client("alice-machine", 1000)

    alice.hello("alice", "a-secret")

    assertEquals(alice.lastPacket, Some(Packet.Accepted))
    assertEquals(f.events.toList, List("connected alice"))
    assert(f.sessions.isConnected("alice"))
    assert(!f.sessions.isConnected("bob"))
  }

  test("credentials that are not good are rejected, and no session is created") {
    val f   = new Fixture
    val eve = f.client("eve-machine", 1000)

    eve.hello("alice", "wrong secret")
    assertEquals(eve.lastPacket, Some(Packet.Rejected(RejectReason.Unauthorized)))
    eve.hello("nobody", "a-secret")
    assertEquals(eve.lastPacket, Some(Packet.Rejected(RejectReason.Unauthorized)))
    eve.hello("alice", "a-secret", gameId = "another game")
    assertEquals(eve.lastPacket, Some(Packet.Rejected(RejectReason.Unauthorized)))

    assertEquals(f.events.toList, Nil)
    assert(!f.sessions.isConnected("alice"))
  }

  test("players are told when the server is not ready for them") {
    val f     = new Fixture
    val alice = f.client("alice-machine", 1000)
    f.ready = false

    alice.hello("alice", "a-secret")
    assertEquals(alice.lastPacket, Some(Packet.Rejected(RejectReason.NotReady)))

    f.ready = true
    alice.hello("alice", "a-secret")
    assertEquals(alice.lastPacket, Some(Packet.Accepted))
  }

  test("data from someone who did not say hello is rejected, and does not reach the game") {
    val f   = new Fixture
    val eve = f.client("eve-machine", 1000)

    // a perfectly valid message, but from a stranger
    eve.connection.sendOrderedReliable(InGameWSProtocol.LetsBegin)

    assertEquals(
      eve.packets.collect { case p: Packet.Rejected => p }.head,
      Packet.Rejected(RejectReason.NoSession)
    )
    assertEquals(f.serverMessages.toList, Nil)
  }

  test("what is not understood is ignored") {
    val f   = new Fixture
    val eve = f.client("eve-machine", 1000)

    eve.sendRaw(Array.emptyByteArray)
    eve.sendRaw(Array[Byte](99, 1, 2, 3))
    eve.sendRaw(Envelope.accepted) // meant for clients

    assertEquals(eve.packets.toList, Nil)
    assertEquals(f.events.toList, Nil)
  }

  test("messages go both ways between a player and the game") {
    val f     = new Fixture
    val alice = f.client("alice-machine", 1000)
    val bob   = f.client("bob-machine", 2000)
    alice.hello("alice", "a-secret")
    bob.hello("bob", "b-secret")

    alice.connection.sendOrderedReliable(InGameWSProtocol.Ready("alice"))
    bob.connection.sendOrderedReliable(InGameWSProtocol.LetsBegin)
    f.sessions.send(
      "alice",
      InGameWSProtocol.AllPlayers(Vector("alice", "bob")),
      Delivery.OrderedReliable
    )
    f.sessions.send("bob", InGameWSProtocol.EveryoneIsReady, Delivery.Reliable)
    f.sessions.send("nobody", InGameWSProtocol.EveryoneIsReady, Delivery.Reliable) // does nothing

    assertEquals(
      f.serverMessages.toList,
      List("alice" -> InGameWSProtocol.Ready("alice"), "bob" -> InGameWSProtocol.LetsBegin)
    )
    assertEquals(alice.messages.toList, List(InGameWSProtocol.AllPlayers(Vector("alice", "bob"))))
    assertEquals(bob.messages.toList, List(InGameWSProtocol.EveryoneIsReady))
  }

  test("saying hello again changes nothing, except that the answer is sent again") {
    val f     = new Fixture
    val alice = f.client("alice-machine", 1000)

    alice.hello("alice", "a-secret")
    alice.hello("alice", "a-secret")
    alice.hello("alice", "a-secret")

    assertEquals(alice.packets.toList, List.fill(3)(Packet.Accepted))
    assertEquals(f.events.toList, List("connected alice"))

    alice.connection.sendOrderedReliable(InGameWSProtocol.LetsBegin)
    assertEquals(f.serverMessages.toList, List("alice" -> InGameWSProtocol.LetsBegin))
  }

  test("a player coming from another address replaces their previous session") {
    val f      = new Fixture
    val before = f.client("alice-machine", 1000)
    val after  = f.client("alice-phone", 3000)
    before.hello("alice", "a-secret")

    after.hello("alice", "a-secret")
    assertEquals(after.lastPacket, Some(Packet.Accepted))
    assertEquals(f.events.toList, List("connected alice", "lost alice", "connected alice"))

    before.connection.sendOrderedReliable(InGameWSProtocol.LetsBegin)
    assertEquals(before.lastPacket, Some(Packet.Rejected(RejectReason.NoSession)))
    assertEquals(f.serverMessages.toList, Nil)

    after.connection.sendOrderedReliable(InGameWSProtocol.LetsBegin)
    assertEquals(f.serverMessages.toList, List("alice" -> InGameWSProtocol.LetsBegin))

    f.sessions.send("alice", InGameWSProtocol.EveryoneIsReady, Delivery.OrderedReliable)
    assertEquals(after.messages.toList, List(InGameWSProtocol.EveryoneIsReady))
    assertEquals(before.messages.toList, Nil)
  }

  test("an address is only for one player at a time") {
    val f      = new Fixture
    val shared = f.client("shared-machine", 1000)

    shared.hello("alice", "a-secret")
    shared.hello("bob", "b-secret")

    assertEquals(f.events.toList, List("connected alice", "lost alice", "connected bob"))
    assert(!f.sessions.isConnected("alice"))
    assert(f.sessions.isConnected("bob"))
    shared.connection.sendOrderedReliable(InGameWSProtocol.LetsBegin)
    assertEquals(f.serverMessages.toList, List("bob" -> InGameWSProtocol.LetsBegin))
  }

  test("a player that goes silent is lost, once, and can come back") {
    val f     = new Fixture
    val alice = f.client("alice-machine", 1000)
    alice.hello("alice", "a-secret")

    alice.online = false
    f.timers.advance(60000)
    assertEquals(f.events.toList, List("connected alice", "lost alice"))
    assert(!f.sessions.isConnected("alice"))
    f.sessions.send("alice", InGameWSProtocol.EveryoneIsReady, Delivery.Reliable) // does nothing
    assertEquals(alice.messages.toList, Nil)

    f.timers.advance(60000)
    assertEquals(f.events.toList, List("connected alice", "lost alice"))

    alice.online = true
    alice.hello("alice", "a-secret")
    assertEquals(f.events.toList, List("connected alice", "lost alice", "connected alice"))
    alice.connection.sendOrderedReliable(InGameWSProtocol.LetsBegin)
    assertEquals(f.serverMessages.toList, List("alice" -> InGameWSProtocol.LetsBegin))
  }

  test("a player that keeps talking is not lost") {
    val f     = new Fixture
    val alice = f.client("alice-machine", 1000)
    alice.hello("alice", "a-secret")

    for _ <- 1 to 30 do {
      f.timers.advance(5000)
      alice.connection.sendNormal(InGameWSProtocol.HeartBeat)
    }

    assertEquals(f.events.toList, List("connected alice"))
    assert(f.sessions.isConnected("alice"))
  }

  test("closing drops the sessions without a word, and closes the socket") {
    val f     = new Fixture
    val alice = f.client("alice-machine", 1000)
    alice.hello("alice", "a-secret")

    f.sessions.close()

    assert(f.network.closed)
    assert(!f.sessions.isConnected("alice"))
    assertEquals(f.events.toList, List("connected alice"))
    f.timers.advance(60000)
    assertEquals(f.events.toList, List("connected alice"))
  }
}
