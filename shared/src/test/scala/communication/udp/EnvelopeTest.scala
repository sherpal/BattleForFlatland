package communication.udp

import communication.udp.Envelope.{Nonce, Packet}

final class EnvelopeTest extends munit.FunSuite {

  private val secret      = Hmac("s3cr3t-é")
  private val clientNonce = Nonce(Array.tabulate[Byte](Nonce.Size)(_.toByte))
  private val serverNonce = Nonce(Array.tabulate[Byte](Nonce.Size)(i => (100 + i).toByte))
  private val nonces      = Packet.Challenge(clientNonce, serverNonce)

  private def roundTrip(packet: Packet): Option[Packet] = Envelope.parse(Envelope.encode(packet))

  test("the handshake packets go through") {
    for packet <- List(
        Packet.Hello("some user", 42, clientNonce),
        nonces,
        Envelope.proof("some user", 42, nonces, secret),
        Packet.Rejected(Envelope.RejectReason.NotReady),
        Packet.Rejected(Envelope.RejectReason.Unauthorized),
        Packet.Rejected(Envelope.RejectReason.NoSession)
      )
    do assertEquals(roundTrip(packet), Some(packet))
  }

  test("the secret never travels") {
    val datagram = Envelope.encode(Envelope.proof("some user", 42, nonces, secret))
    assert(!datagram.toSeq.containsSlice(Hmac.utf8("s3cr3t-é").toSeq))
  }

  test("a proof is only good with the secret, for the same nonces and player") {
    val proof = Envelope.proof("some user", 42, nonces, secret)
    assert(Envelope.isProven(proof, secret))
    assert(!Envelope.isProven(proof, Hmac("another secret")))
    assert(!Envelope.isProven(proof.copy(userName = "someone else"), secret))
    assert(!Envelope.isProven(proof.copy(nonces = nonces.copy(serverNonce = clientNonce)), secret))
  }

  test("challenges are made for an address, and do not last") {
    var now        = 1_000_000_000L
    val challenges = Envelope.Challenges(Hmac("server key"), () => now)
    val from       = Peer("81.12.34.56", 40000)
    val hello      = Packet.Hello("some user", 42, clientNonce)
    val challenge  = challenges.challenge(hello, from)
    val proof      = Envelope.proof("some user", 42, challenge, secret)
    assert(challenges.isOurs(proof, from))
    assert(!challenges.isOurs(proof, Peer("81.12.34.56", 40001)), "another port")
    assert(!challenges.isOurs(proof, Peer("1.2.3.4", 40000)), "another address")
    assert(!Envelope.Challenges(Hmac("another key"), () => now).isOurs(proof, from))
    now += 60000
    assert(challenges.isOurs(proof, from), "a minute later")
    now += 60000
    assert(!challenges.isOurs(proof, from), "two minutes later")
  }

  test("data goes from one side of the session to the other, only once") {
    val player  = Envelope.SessionKeys(secret, nonces, towardsServer = true)
    val server  = Envelope.SessionKeys(secret, nonces, towardsServer = false)
    val payload = Array.tabulate[Byte](300)(i => (i * 7).toByte)

    val datagram = player.seal(payload)
    val data     = Envelope.parse(datagram).collect { case d: Packet.Data => d }.get
    assertEquals(server.open(data).map(_.toList), Some(payload.toList))
    assertEquals(server.open(data), None, "not twice")
    assertEquals(player.open(data), None, "not back to its sender")
    val empty =
      Envelope.parse(server.seal(Array.emptyByteArray)).collect { case d: Packet.Data => d }
    assertEquals(empty.flatMap(player.open).map(_.length), Some(0))
  }

  test("data of another session, or tampered with, is dropped") {
    val player   = Envelope.SessionKeys(secret, nonces, towardsServer = true)
    val server   = Envelope.SessionKeys(secret, nonces, towardsServer = false)
    val other    = Envelope.SessionKeys(secret, nonces.copy(serverNonce = clientNonce), false)
    val datagram = player.seal(Array[Byte](1, 2, 3))
    val data     = Envelope.parse(datagram).collect { case d: Packet.Data => d }.get
    assertEquals(other.open(data), None)
    datagram(10) = (datagram(10) ^ 1).toByte
    assertEquals(
      Envelope.parse(datagram).collect { case d: Packet.Data => d }.flatMap(server.open),
      None
    )
  }

  test("only the game server can accept") {
    val player   = Envelope.SessionKeys(secret, nonces, towardsServer = true)
    val server   = Envelope.SessionKeys(secret, nonces, towardsServer = false)
    val accepted = roundTrip(server.accepted).collect { case a: Packet.Accepted => a }.get
    assert(player.isAccepted(accepted))
    assert(!player.isAccepted(Envelope.SessionKeys(Hmac("guess"), nonces, false).accepted))
  }

  test("the replay window takes the late datagrams once, and not the too old ones") {
    val window = Envelope.ReplayWindow()
    assert(window.accept(1))
    assert(window.accept(3))
    assert(window.accept(2), "late")
    assert(!window.accept(2), "again")
    assert(window.accept(100))
    assert(window.accept(37), "63 behind")
    assert(!window.accept(36), "64 behind")
    assert(!window.accept(0))
  }

  test("what is not an envelope is ignored") {
    assertEquals(Envelope.parse(Array.emptyByteArray), None)
    assertEquals(Envelope.parse(Array[Byte](99, 1, 2, 3)), None)
    assertEquals(Envelope.parse(Array[Byte](0)), None)
    assertEquals(Envelope.parse(Array[Byte](3)), None, "rejected without a reason")
    assertEquals(Envelope.parse(Array[Byte](3, 1, 1)), None, "rejected with too much")
    assertEquals(Envelope.parse(Array[Byte](4, 1, 2, 3)), None, "data without a tag")
    assertEquals(Envelope.parse(Array[Byte](5, 1, 2, 3)), None, "challenge too short")
  }

  test("a hello too big to be one is ignored") {
    val huge = Packet.Hello("a" * 1000, 1, clientNonce)
    assertEquals(Envelope.parse(Envelope.encode(huge)), None)
  }

  test("the data socket seals what is sent, and only that") {
    val underlying = new FakeSocket(Peer("localhost", 5))
    val socket     = new Envelope.DataSocket(underlying, Envelope.SessionKeys(secret, nonces, true))
    val server     = Envelope.SessionKeys(secret, nonces, towardsServer = false)

    socket.send(Array[Byte](1, 2, 3), 42, "1.2.3.4")

    assertEquals(socket.peer, underlying.peer)
    assertEquals(underlying.sent.map(s => (s.port, s.address)).toList, List((42, "1.2.3.4")))
    assertEquals(
      Envelope
        .parse(underlying.takeSent())
        .collect { case d: Packet.Data => d }
        .flatMap(server.open)
        .map(_.toList),
      Some(List[Byte](1, 2, 3))
    )
  }
}
