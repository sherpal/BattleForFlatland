package communication.udp

import communication.udp.Rendezvous.Packet

final class RendezvousTest extends munit.FunSuite {

  private val key = Hmac("s3cr3t-é")

  test("every packet goes through, signed") {
    for packet <- List(
        Packet.ServerRegister(42, 1, None),
        Packet.ServerRegister(42, 1730000000000L, Some(Peer("81.12.34.56", 40000))),
        Packet.Players(
          Packet.Registered(Peer("127.0.0.1", 22222), Some(Peer("81.12.34.56", 40000))),
          Vector("alice" -> Peer("81.12.34.56", 40000), "bob" -> Peer("::1", 1))
        ),
        Packet.Players(Packet.Registered(Peer("81.12.34.56", 40000), None), Vector.empty),
        Packet.PlayerRegister("some user", 42, 2, None),
        Packet.PlayerRegister("some user", 42, 3, Some(Peer("81.12.34.56", 40000))),
        Packet.Registered(Peer("81.12.34.56", 40000), None),
        Packet.Registered(Peer("192.168.1.12", 40000), Some(Peer("81.12.34.56", 40001)))
      )
    do {
      val datagram = Rendezvous.encode(packet, key)
      assert(Rendezvous.isRendezvous(datagram), packet)
      val received = Rendezvous.parse(datagram)
      assertEquals(received.map(_.packet), Some(packet))
      assert(received.exists(_.signedWith(key)), packet)
      assert(!received.exists(_.signedWith(Hmac("another key"))), packet)
    }
  }

  test("punches carry nothing, and are not signed") {
    assertEquals(Rendezvous.parse(Rendezvous.punch).map(_.packet), Some(Packet.Punch))
    assert(!Rendezvous.parse(Rendezvous.punch).exists(_.signedWith(key)))
  }

  test("the clients only decode what is signed with their key") {
    val registered = Packet.Registered(Peer("81.12.34.56", 40000), None)
    val datagram   = Rendezvous.encode(registered, key)
    assertEquals(Rendezvous.parseSignedWith(datagram, key), Some(registered))
    assertEquals(Rendezvous.parseSignedWith(datagram, Hmac("another key")), None)
    assertEquals(Rendezvous.parseSignedWith(Rendezvous.punch, key), Some(Packet.Punch))
    assertEquals(Rendezvous.parseSignedWith(Array[Byte](19, 1, 2), key), None)
  }

  test("a tampered register is not signed anymore") {
    val datagram = Rendezvous.encode(Packet.PlayerRegister("some user", 42, 2, None), key)
    datagram(3) = (datagram(3) ^ 1).toByte
    assert(!Rendezvous.parse(datagram).exists(_.signedWith(key)))
  }

  test("the numbers of the registers increase") {
    val counter = Rendezvous.Counter()
    val first   = counter.next()
    assert(counter.next() > first)
    assert(Rendezvous.Counter().next() >= first, "the time, when the program runs again")
  }

  test("rendezvous and envelope datagrams are told apart") {
    val nonce = Envelope.Nonce(Array.fill[Byte](Envelope.Nonce.Size)(1))
    for datagram <- List(
        Envelope.encode(Envelope.Packet.Hello("some user", 42, nonce)),
        Envelope.encode(Envelope.Packet.Challenge(nonce, nonce)),
        Envelope.encode(Envelope.Packet.Rejected(Envelope.RejectReason.NoSession)),
        Envelope.SessionKeys(key, Envelope.Packet.Challenge(nonce, nonce), true).seal(Array(16, 17))
      )
    do {
      assert(!Rendezvous.isRendezvous(datagram))
      assertEquals(Rendezvous.parse(datagram), None)
    }
    assert(!Rendezvous.isRendezvous(Array.emptyByteArray))
  }

  test("garbage is ignored") {
    assertEquals(Rendezvous.parse(Array[Byte](16, 1, 2)), None)
    assertEquals(Rendezvous.parse(Array.fill[Byte](Rendezvous.MaxSize + 1)(19)), None)
  }
}
