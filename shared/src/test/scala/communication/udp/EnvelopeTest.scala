package communication.udp

import communication.udp.Envelope.Packet
import models.bff.ingame.GameUserCredentials

final class EnvelopeTest extends munit.FunSuite {

  private val credentials = GameUserCredentials("some user", "game-id", "s3cr3t-é")

  test("hello carries the credentials") {
    assertEquals(Envelope.parse(Envelope.hello(credentials)), Some(Packet.Hello(credentials)))
  }

  test("accepted and rejected") {
    assertEquals(Envelope.parse(Envelope.accepted), Some(Packet.Accepted))
    for reason <- List(
        Envelope.RejectReason.NotReady,
        Envelope.RejectReason.Unauthorized,
        Envelope.RejectReason.NoSession
      )
    do assertEquals(Envelope.parse(Envelope.rejected(reason)), Some(Packet.Rejected(reason)))
  }

  test("data carries the payload untouched") {
    val payload = Array.tabulate[Byte](300)(i => (i * 7).toByte)
    Envelope.parse(Envelope.data(payload)) match {
      case Some(Packet.Data(received)) => assertEquals(received.toList, payload.toList)
      case other => fail(s"unexpected $other")
    }
    Envelope.parse(Envelope.data(Array.emptyByteArray)) match {
      case Some(Packet.Data(received)) => assertEquals(received.length, 0)
      case other => fail(s"unexpected $other")
    }
  }

  test("what is not an envelope is ignored") {
    assertEquals(Envelope.parse(Array.emptyByteArray), None)
    assertEquals(Envelope.parse(Array[Byte](99, 1, 2, 3)), None)
    assertEquals(Envelope.parse(Array[Byte](0)), None)
    assertEquals(Envelope.parse(Array[Byte](3)), None, "rejected without a reason")
    assertEquals(Envelope.parse(Array[Byte](3, 1, 1)), None, "rejected with too much")
  }

  test("a hello too big to be one is ignored") {
    val huge = GameUserCredentials("a" * 1000, "b", "c")
    assertEquals(Envelope.parse(Envelope.hello(huge)), None)
  }

  test("the data socket wraps what is sent, and only that") {
    val underlying = new FakeSocket(Peer("localhost", 5))
    val socket     = new Envelope.DataSocket(underlying)

    socket.send(Array[Byte](1, 2, 3), 42, "1.2.3.4")

    assertEquals(socket.peer, underlying.peer)
    assertEquals(underlying.sent.map(s => (s.port, s.address)).toList, List((42, "1.2.3.4")))
    assertEquals(
      Envelope.parse(underlying.takeSent()).collect { case Packet.Data(p) => p.toList },
      Some(List[Byte](1, 2, 3))
    )
  }
}
