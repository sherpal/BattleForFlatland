package communication.udp

// Decoding arbitrary bytes is only guarded on the JVM, which is where the server (the one with a port open
// to the world) runs: on Scala.js, a truncated packet makes boopickle read out of bounds, which is an
// undefined behaviour that cannot be caught. Clients should therefore only feed `onMessage` with packets
// coming from the server's address.
final class ConnectionJvmTest extends munit.FunSuite {

  test("undecodable packets are ignored") {
    val sender   = new Harness
    val receiver = new Harness

    sender.conn.sendNormal(Text("a message that is long enough to be cut"))
    val valid = sender.socket.takeSent()

    receiver.conn.onMessage(Array.emptyByteArray)
    receiver.conn.onMessage(Array[Byte](127, 127, 127, 127))
    receiver.conn.onMessage(valid.take(valid.length / 2))

    assertEquals(receiver.received.toList, Nil)
    assertEquals(receiver.socket.sent.size, 0)

    receiver.conn.onMessage(valid)
    assertEquals(receiver.received.size, 1, "the connection is still usable")
  }

  test("garbage is not a sign of life of the peer") {
    val h = new Harness

    h.conn.connect(pingDelay = 100000)
    for _ <- 1 to 10 do {
      h.sim.advance(2500)
      h.conn.onMessage(Array[Byte](127, 127, 127))
    }

    assert(!h.conn.connected)
  }
}
