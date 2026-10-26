package communication.udp

final class ConnectionTest extends munit.FunSuite {

  private def texts(values: String*): List[TestMessage] = values.map(Text(_)).toList

  test("normal messages are delivered as they come, with no ack nor retransmission") {
    val sender   = new Harness
    val receiver = new Harness

    sender.conn.sendNormal(Text("hello"))
    assertEquals(sender.socket.sent.size, 1)
    val packet = sender.socket.takeSent()

    receiver.conn.onMessage(packet)
    receiver.conn.onMessage(packet) // normal messages are not deduplicated

    assertEquals(receiver.received.toList, texts("hello", "hello"))
    assertEquals(receiver.socket.sent.size, 0)
    sender.sim.advance(5000)
    assertEquals(sender.socket.sent.size, 0)
  }

  test("messages are sent to the peer of the connection, or to the given one") {
    val h = new Harness(Peer("10.0.0.1", 4242))

    h.conn.sendNormal(Text("a"))
    h.conn.sendReliableTo(Text("b"), Peer("10.0.0.2", 99))

    assertEquals(h.socket.sent.map(s => (s.address, s.port)).toList, List(("10.0.0.1", 4242), ("10.0.0.2", 99)))
  }

  test("ordered messages discard stale and duplicate ids") {
    val sender   = new Harness
    val receiver = new Harness

    sender.conn.sendOrdered(Text("first"))
    val firstPacket = sender.socket.takeSent()
    sender.conn.sendOrdered(Text("second"))
    val secondPacket = sender.socket.takeSent()

    receiver.conn.onMessage(secondPacket)
    receiver.conn.onMessage(firstPacket)
    receiver.conn.onMessage(secondPacket)

    assertEquals(receiver.received.toList, texts("second"))
  }

  test("reliable messages are acknowledged and delivered only once") {
    val sender   = new Harness
    val receiver = new Harness

    sender.conn.sendReliable(Text("once"))
    val messagePacket = sender.socket.takeSent()
    receiver.conn.onMessage(messagePacket)
    val ackPacket = receiver.socket.takeSent()
    sender.conn.onMessage(ackPacket)
    receiver.conn.onMessage(messagePacket)
    assertEquals(receiver.socket.sent.size, 1, "the duplicate must be acknowledged again")

    assertEquals(receiver.received.toList, texts("once"))
    sender.sim.advance(5000)
    assertEquals(sender.socket.sent.size, 0, "no retransmission once acknowledged")
  }

  test("reliable messages are delivered once whatever the order and the duplication of packets") {
    val sender   = new Harness
    val receiver = new Harness

    val packets = (0 until 4).map { i =>
      sender.conn.sendReliable(Text(s"m$i"))
      sender.socket.takeSent()
    }

    for i <- List(2, 0, 2, 3, 1, 0, 3, 1) do receiver.conn.onMessage(packets(i))

    assertEquals(receiver.received.toList, texts("m2", "m0", "m3", "m1"))
    assertEquals(receiver.socket.sent.size, 8, "every packet is acknowledged, duplicates included")
  }

  test("reliable messages are retransmitted until acknowledged") {
    val sender   = new Harness
    val receiver = new Harness

    sender.conn.sendReliable(Text("persistent"))
    val first = sender.socket.takeSent() // lost
    assertEquals(sender.socket.sent.size, 0)

    sender.sim.advance(2000)
    val retransmissions = sender.socket.sent.size
    assert(retransmissions >= 2, s"expected several retransmissions, got $retransmissions")
    assert(retransmissions < 100, s"retransmissions must be spaced out, got $retransmissions")

    receiver.conn.onMessage(sender.socket.takeSent())
    sender.conn.onMessage(receiver.socket.takeSent()) // the ack
    sender.socket.sent.clear()
    sender.sim.advance(2000)

    assertEquals(sender.socket.sent.size, 0)
    assertEquals(receiver.received.toList, texts("persistent"))
    // the retransmitted packet is the same as the original, so the receiver can recognize it
    receiver.conn.onMessage(first)
    assertEquals(receiver.received.toList, texts("persistent"))
  }

  test("ordered reliable messages wait for gaps and then drain in order") {
    val sender   = new Harness
    val receiver = new Harness

    sender.conn.sendOrderedReliable(Text("first"))
    val firstPacket = sender.socket.takeSent()
    sender.conn.sendOrderedReliable(Text("second"))
    val secondPacket = sender.socket.takeSent()
    sender.conn.sendOrderedReliable(Text("third"))
    val thirdPacket = sender.socket.takeSent()

    receiver.conn.onMessage(thirdPacket)
    receiver.conn.onMessage(secondPacket)
    receiver.socket.sent.clear() // ACKs
    assertEquals(receiver.received.toList, Nil)

    receiver.conn.onMessage(firstPacket)
    assertEquals(receiver.received.toList, texts("first", "second", "third"))
    assertEquals(receiver.socket.sent.size, 1)

    receiver.conn.onMessage(secondPacket)
    assertEquals(receiver.socket.sent.size, 2, "duplicates are acknowledged")
    assertEquals(receiver.received.toList, texts("first", "second", "third"))
  }

  test("ordered reliable messages are retransmitted until acknowledged") {
    val sender   = new Harness
    val receiver = new Harness

    sender.conn.sendOrderedReliable(Text("persistent"))
    sender.socket.takeSent() // lost
    sender.sim.advance(2000)
    assert(sender.socket.sent.nonEmpty)

    receiver.conn.onMessage(sender.socket.takeSent())
    sender.conn.onMessage(receiver.socket.takeSent()) // the ack
    sender.socket.sent.clear()
    sender.sim.advance(2000)

    assertEquals(sender.socket.sent.size, 0)
    assertEquals(receiver.received.toList, texts("persistent"))
  }

  test("nothing watches over the connection until it is connected") {
    val h = new Harness

    assert(!h.conn.connected)
    h.sim.advance(120000)
    assert(!h.conn.connected)
    assertEquals(h.socket.sent.size, 0, "no ping")

    h.conn.connect()
    assert(h.conn.connected)
  }

  test("the connection is lost when the peer is silent for too long") {
    val h = new Harness

    h.conn.connect()
    assert(h.conn.connected)
    h.sim.advance(9000)
    assert(h.conn.connected)
    h.sim.advance(20000)
    assert(!h.conn.connected)
  }

  test("the connection stays alive as long as the peer talks") {
    val sender   = new Harness
    val receiver = new Harness

    sender.conn.sendNormal(Text("ping"))
    val packet = sender.socket.takeSent()

    receiver.conn.connect(pingDelay = 100000)
    for _ <- 1 to 30 do {
      receiver.sim.advance(5000)
      receiver.conn.onMessage(packet)
    }
    assert(receiver.conn.connected)

    receiver.sim.advance(30000)
    assert(!receiver.conn.connected)
    receiver.conn.onMessage(packet)
    assert(!receiver.conn.connected, "a lost connection does not come back")
  }

  test("reliable messages are not retransmitted once the connection is lost") {
    val h = new Harness

    h.conn.connect()
    h.conn.sendReliable(Text("nobody listens"))
    h.sim.advance(30000)
    assert(!h.conn.connected)
    h.socket.sent.clear()
    h.sim.advance(30000)

    assertEquals(h.socket.sent.size, 0, "neither reliable messages nor pings are sent")
  }

  test("connecting again after the connection was lost starts over") {
    val h = new Harness

    h.conn.connect()
    h.sim.advance(30000)
    assert(!h.conn.connected)

    h.conn.connect()
    assert(h.conn.connected)
    h.socket.sent.clear()
    h.sim.advance(2500)
    assertEquals(h.socket.sent.size, 2, "pings are back")
    h.sim.advance(30000)
    assert(!h.conn.connected, "and the connection is monitored again")
  }

  test("connecting twice does not double the pings nor the checks") {
    val h = new Harness

    h.conn.connect(1000)
    h.conn.connect(1000)
    h.sim.advance(3500)

    assertEquals(h.socket.sent.size, 3)
    h.sim.advance(30000)
    assert(!h.conn.connected)
  }

  test("pinging sends a ping at the requested interval, until activated again") {
    val h = new Harness

    h.conn.activatePing(1000)
    h.sim.advance(3500)
    assertEquals(h.socket.sent.size, 3)

    h.socket.sent.clear()
    h.conn.activatePing(500) // replaces the previous interval
    h.sim.advance(2000)
    assertEquals(h.socket.sent.size, 4)
  }

  test("link time uses the configured offset") {
    val local  = new Harness
    val remote = new Harness

    local.conn.computeLinkTime(sampleTime = 1, sampleNumber = 1, _ => ())
    remote.conn.onMessage(local.socket.takeSent())
    local.conn.onMessage(remote.socket.takeSent())

    assertEquals(local.conn.linkTime, local.timers.now())
  }
}
