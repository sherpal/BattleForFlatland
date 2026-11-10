package server

import communication.udp.{Hmac, Lobby, Peer, Rendezvous, UDPSocket}
import communication.udp.Rendezvous.Packet
import menus.data.{
  AllGameCredentials,
  GameCredentials,
  LobbyMessage,
  LobbySubscription,
  MenuGameComm,
  User
}
import models.bff.ingame.GameUserCredentials
import services.events.Events

import scala.collection.mutable

final class RendezvousServerTest extends munit.FunSuite {

  /** Remembers what is sent, and receives what it is given. */
  private final class FakeSocket extends UDPSocket {
    val sent = mutable.Buffer.empty[(Array[Byte], Peer)]

    def peer: Peer                                     = Peer("0.0.0.0", 9000)
    def bind(port: Int, address: Option[String]): Unit = ()
    def send(message: Array[Byte], port: Int, address: String): Unit =
      sent += ((message, Peer(address, port)))
    def close(): Unit = ()

    def receive(datagram: Array[Byte], from: Peer): Unit = onMessage(datagram, from)
  }

  private val gameSecret = Hmac("game secret")
  private val aliceKey   = Hmac("alice secret")
  private val credentials = AllGameCredentials(
    GameCredentials("g", "game secret"),
    Vector(GameUserCredentials("alice", "g", "alice secret"))
  )

  private def fixture() = {
    val socket = FakeSocket()
    val server = RendezvousServer(socket, gameId => Option.when(gameId == "g")(credentials))
    server.start(9000, None)
    (socket, server)
  }

  private val aliceAt  = Peer("81.12.34.56", 40000)
  private val serverAt = Peer("1.2.3.4", 50000)

  private def register(number: Long) = Packet.PlayerRegister("alice", "g", number, None)

  test("a player signed with their secret is registered, and told so in the same way") {
    val (socket, _) = fixture()
    socket.receive(Rendezvous.encode(register(10), aliceKey), aliceAt)
    assertEquals(socket.sent.map(_._2).toList, List(aliceAt))
    assertEquals(
      Rendezvous.parseSignedWith(socket.sent.head._1, aliceKey),
      Some(Packet.Registered(aliceAt, None))
    )
  }

  test("the game server learns where the players are") {
    val (socket, server) = fixture()
    socket.receive(Rendezvous.encode(register(10), aliceKey), aliceAt)
    socket.receive(Rendezvous.encode(Packet.ServerRegister("g", 1, None), gameSecret), serverAt)
    assertEquals(
      socket.sent.lastOption.flatMap((bytes, _) => Rendezvous.parseSignedWith(bytes, gameSecret)),
      Some(Packet.Players(Packet.Registered(serverAt, None), Vector("alice" -> aliceAt)))
    )
    assertEquals(server.gameServerAddresses("g"), Vector(serverAt))
  }

  test("registers that are not signed with the right secret are ignored") {
    val (socket, server) = fixture()
    socket.receive(Rendezvous.encode(register(10), Hmac("guess")), aliceAt)
    socket.receive(Rendezvous.encode(register(10), gameSecret), aliceAt)
    socket.receive(Rendezvous.encode(Packet.ServerRegister("g", 1, None), aliceKey), serverAt)
    socket.receive(Rendezvous.encode(Packet.ServerRegister("other", 1, None), gameSecret), serverAt)
    assertEquals(socket.sent.toList, Nil)
    assertEquals(server.gameServerAddresses("g"), Vector.empty)
  }

  test("a register replayed from elsewhere does not divert the player") {
    val (socket, _) = fixture()
    val datagram    = Rendezvous.encode(register(10), aliceKey)
    socket.receive(datagram, aliceAt)
    socket.receive(datagram, Peer("6.6.6.6", 666))
    socket.receive(Rendezvous.encode(register(9), aliceKey), Peer("6.6.6.6", 666))
    socket.receive(Rendezvous.encode(Packet.ServerRegister("g", 1, None), gameSecret), serverAt)
    assertEquals(
      socket.sent.lastOption.flatMap((bytes, _) => Rendezvous.parseSignedWith(bytes, gameSecret)),
      Some(Packet.Players(Packet.Registered(serverAt, None), Vector("alice" -> aliceAt)))
    )
    // a newer one does: the player moved
    socket.receive(Rendezvous.encode(register(11), aliceKey), Peer("81.12.34.56", 40001))
    socket.receive(Rendezvous.encode(Packet.ServerRegister("g", 2, None), gameSecret), serverAt)
    assertEquals(
      socket.sent.lastOption.flatMap((bytes, _) => Rendezvous.parseSignedWith(bytes, gameSecret)),
      Some(
        Packet.Players(
          Packet.Registered(serverAt, None),
          Vector("alice" -> Peer("81.12.34.56", 40001))
        )
      )
    )
  }
}

final class LobbyServerTest extends munit.FunSuite {
  private final class FakeSocket extends UDPSocket {
    val sent = mutable.Buffer.empty[(Array[Byte], Peer)]

    def peer: Peer                                     = Peer("0.0.0.0", 9000)
    def bind(port: Int, address: Option[String]): Unit = ()
    def send(message: Array[Byte], port: Int, address: String): Unit =
      sent += ((message, Peer(address, port)))
    def close(): Unit = ()
  }

  private val alice = User("alice")
  private val from  = Peer("81.12.34.56", 40000)

  private def fixture(): (FakeSocket, LobbyServer, LobbySubscription) = {
    val socket = FakeSocket()
    val lobby  = LobbyServer(socket, 9000, Events())
    (socket, lobby, lobby.subscribe(alice, Some("g")))
  }

  private def hello(lobby: LobbyServer, subscription: LobbySubscription, number: Long, key: Hmac) =
    Lobby
      .parse(Lobby.encode(Lobby.Packet.Hello(subscription.id, number), key))
      .foreach(lobby.received(_, from))

  test("a signed hello is answered with a signed poke, to where it came from") {
    val (socket, lobby, subscription) = fixture()
    val key                           = Hmac(subscription.key)
    hello(lobby, subscription, 1, key)
    assertEquals(socket.sent.map(_._2).toList, List(from))
    assertEquals(
      Lobby.parseSignedWith(socket.sent.head._1, key),
      Some(Lobby.Packet.Poke(subscription.id, 0))
    )
  }

  test("hellos not signed with the key of the subscription, or replayed, are ignored") {
    val (socket, lobby, subscription) = fixture()
    hello(lobby, subscription, 1, Hmac("guess"))
    assertEquals(socket.sent.toList, Nil)
    hello(lobby, subscription, 5, Hmac(subscription.key))
    hello(lobby, subscription, 5, Hmac(subscription.key))
    hello(lobby, subscription, 4, Hmac(subscription.key))
    assertEquals(socket.sent.size, 1)
  }

  test("a DataUpdated is merged with the one not fetched yet, not with one already fetched") {
    val (socket, lobby, subscription) = fixture()
    val key                           = Hmac(subscription.key)
    hello(lobby, subscription, 1, key)
    def latestPoked =
      socket.sent.lastOption.flatMap((bytes, _) => Lobby.parseSignedWith(bytes, key))

    lobby.push(subscription.id, Vector(MenuGameComm.DataUpdated()))
    lobby.push(subscription.id, Vector(MenuGameComm.DataUpdated()))
    assertEquals(latestPoked, Some(Lobby.Packet.Poke(subscription.id, 1)))
    assertEquals(
      lobby.messages(alice, subscription.id, 0),
      Some(Vector(LobbyMessage(1, MenuGameComm.DataUpdated())))
    )

    // e.g. AIs added after the client fetched: it must hear about it
    lobby.push(subscription.id, Vector(MenuGameComm.DataUpdated()))
    assertEquals(latestPoked, Some(Lobby.Packet.Poke(subscription.id, 2)))
    assertEquals(
      lobby.messages(alice, subscription.id, 1),
      Some(Vector(LobbyMessage(2, MenuGameComm.DataUpdated())))
    )
  }

  test("the messages are only for the user of the subscription") {
    val (_, lobby, subscription) = fixture()
    assertEquals(lobby.messages(alice, subscription.id, 0), Some(Vector.empty))
    assertEquals(lobby.messages(User("eve"), subscription.id, 0), None)
    assertEquals(lobby.messages(alice, "no such subscription", 0), None)
    lobby.unsubscribe(alice, subscription.id)
    assertEquals(lobby.messages(alice, subscription.id, 0), None)
  }
}
