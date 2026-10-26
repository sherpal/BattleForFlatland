package communication.udp

import java.util.concurrent.{CountDownLatch, LinkedBlockingQueue, TimeUnit}

final class JvmUDPSocketTest extends munit.FunSuite {

  private val loopback = Some("127.0.0.1")

  private final class Listening {
    val socket    = new JvmUDPSocket
    val received  = new LinkedBlockingQueue[(List[Byte], Peer)]()
    val closed    = new CountDownLatch(1)
    val listening = new CountDownLatch(1)

    socket.setMessageCallback((bytes, from) => received.put((bytes.toList, from)))
    socket.setCloseCallback(() => closed.countDown())
    socket.setListeningCallback(() => listening.countDown())
    socket.bind(0, loopback)

    def next(): Option[(List[Byte], Peer)] = Option(received.poll(5, TimeUnit.SECONDS))
  }

  test("datagrams travel from a socket to another, with the address of the sender") {
    val a = new Listening
    val b = new Listening
    assert(a.listening.await(5, TimeUnit.SECONDS))

    a.socket.send(Array[Byte](1, 2, 3), b.socket.peer.port, "127.0.0.1")
    assertEquals(b.next(), Some((List[Byte](1, 2, 3), a.socket.peer)))

    b.socket.send(Array[Byte](4), a.socket.peer.port, "127.0.0.1")
    assertEquals(a.next(), Some((List[Byte](4), b.socket.peer)))

    a.socket.close()
    b.socket.close()
  }

  test("the system chooses the port when asked to bind to 0") {
    val a = new Listening
    assert(a.socket.peer.port > 0)
    assertEquals(a.socket.peer.address, "127.0.0.1")
    a.socket.close()
  }

  test("datagrams are received with their exact content, empty or big") {
    val a   = new Listening
    val b   = new Listening
    val big = Array.tabulate[Byte](8000)(i => (i % 251).toByte)

    a.socket.send(Array.emptyByteArray, b.socket.peer.port, "127.0.0.1")
    a.socket.send(big, b.socket.peer.port, "127.0.0.1")

    assertEquals(b.next().map(_._1), Some(Nil))
    assertEquals(b.next().map(_._1), Some(big.toList))
    a.socket.close()
    b.socket.close()
  }

  test("closing tells so, and stops the reception") {
    val a = new Listening
    a.socket.close()

    assert(a.closed.await(5, TimeUnit.SECONDS))
  }

  test("a failing callback does not stop the reception") {
    val socket = new JvmUDPSocket
    val latch  = new CountDownLatch(2)
    socket.setMessageCallback { (_, _) =>
      latch.countDown()
      if latch.getCount == 1 then throw new IllegalStateException("expected by the test")
    }
    socket.bind(0, loopback)
    val other = new Listening

    other.socket.send(Array[Byte](1), socket.peer.port, "127.0.0.1")
    other.socket.send(Array[Byte](2), socket.peer.port, "127.0.0.1")

    assert(latch.await(5, TimeUnit.SECONDS))
    socket.close()
    other.socket.close()
  }

  test("it works with connections, through the envelope") {
    import TestMessage.given
    val a        = new Listening
    val b        = new Listening
    val received = new LinkedBlockingQueue[TestMessage]()
    val connection = new Connection[TestMessage](
      b.socket.peer,
      new Envelope.DataSocket(a.socket),
      new TimersImpl,
      _ => ()
    )
    val server = new Connection[TestMessage](
      a.socket.peer,
      new Envelope.DataSocket(b.socket),
      new TimersImpl,
      received.put(_)
    )

    connection.sendOrderedReliable(Text("through the real thing"))
    val datagram = b.next().map(_._1.toArray)
    datagram.flatMap(Envelope.parse(_)) match {
      case Some(Envelope.Packet.Data(payload)) => server.onMessage(payload)
      case other => fail(s"unexpected $other")
    }

    assertEquals(received.poll(5, TimeUnit.SECONDS), Text("through the real thing"))
    a.socket.close()
    b.socket.close()
  }
}
