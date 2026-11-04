package communication.udp

final class LobbyTest extends munit.FunSuite {

  private val key = Hmac("the key of the subscription")

  test("hellos and pokes go through, signed") {
    for packet <- List(Lobby.Packet.Hello("abc", 1730000000000L), Lobby.Packet.Poke("abc", 0)) do {
      val received = Lobby.parse(Lobby.encode(packet, key))
      assertEquals(received.map(_.packet), Some(packet))
      assert(received.exists(_.signedWith(key)))
      assert(!received.exists(_.signedWith(Hmac("another key"))))
    }
  }

  test("the clients only decode what is signed with their key") {
    val poke = Lobby.Packet.Poke("abc", 3)
    assertEquals(Lobby.parseSignedWith(Lobby.encode(poke, key), key), Some(poke))
    assertEquals(Lobby.parseSignedWith(Lobby.encode(poke, Hmac("other")), key), None)
    assertEquals(Lobby.parseSignedWith(Array.fill[Byte](20)(33), key), None)
  }

  test("they are told apart from the others") {
    val hello = Lobby.encode(Lobby.Packet.Hello("abc", 1), key)
    assert(Lobby.isLobby(hello))
    assert(!Rendezvous.isRendezvous(hello))
    assert(!Stun.isStun(hello))
    assert(!Lobby.isLobby(Rendezvous.punch))
    assert(!Lobby.isLobby(Rendezvous.encode(Rendezvous.Packet.ServerRegister("g", 1, None), key)))
  }

  test("the shared socket gives the lobby its datagrams, and the others to the rest") {
    val underlying = new FakeSocket(Peer("localhost", 5))
    var lobby      = List.empty[Lobby.Packet]
    var others     = List.empty[List[Byte]]
    val socket     = Lobby.SharedSocket(underlying, (received, _) => lobby :+= received.packet)
    socket.setMessageCallback((datagram, _) => others :+= datagram.toList)

    underlying.onMessage(Lobby.encode(Lobby.Packet.Hello("abc", 1), key), Peer("1.2.3.4", 1))
    underlying.onMessage(Rendezvous.punch, Peer("1.2.3.4", 1))

    assertEquals(lobby, List(Lobby.Packet.Hello("abc", 1)))
    assertEquals(others, List(Rendezvous.punch.toList))
  }
}
