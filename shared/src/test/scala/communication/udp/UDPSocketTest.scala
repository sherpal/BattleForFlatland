package communication.udp

import scala.collection.mutable

final class UDPSocketTest extends munit.FunSuite {

  // a platform only has to say how to bind, send and close
  private final class MinimalSocket extends UDPSocket {
    val peer: Peer                                                       = Peer("localhost", 1)
    def bind(port: Int, address: Option[String]): Unit                   = ()
    def send(message: Array[Byte], port: Int, address: String): Unit     = ()
    def close(): Unit                                                    = ()
  }

  test("callbacks do nothing until set") {
    val socket = new MinimalSocket

    socket.onMessage(Array.emptyByteArray, Peer("a", 1))
    socket.onClose()
    socket.onListening()
  }

  test("callbacks are the ones last set") {
    val socket = new MinimalSocket
    val events = mutable.Buffer.empty[String]

    socket.setMessageCallback((bytes, peer) => events += s"message ${bytes.length} ${peer.address}:${peer.port}")
    socket.setCloseCallback(() => events += "close")
    socket.setListeningCallback(() => events += "listening")
    socket.setCloseCallback(() => events += "closed again")

    socket.onListening()
    socket.onMessage(Array[Byte](1, 2, 3), Peer("a", 1))
    socket.onClose()

    assertEquals(events.toList, List("listening", "message 3 a:1", "closed again"))
  }
}
