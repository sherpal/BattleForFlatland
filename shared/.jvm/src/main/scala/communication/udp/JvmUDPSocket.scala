package communication.udp

import java.net.{DatagramPacket, DatagramSocket, InetAddress, InetSocketAddress, SocketException}
import java.util.concurrent.ConcurrentHashMap
import scala.util.control.NonFatal

/** A [[UDPSocket]] on top of `java.net.DatagramSocket`. Datagrams are received on a dedicated
  * daemon thread, which is where the message callback is called: it should return quickly, and must
  * not throw (if it does, the exception is printed and the datagram is dropped). Sending can be
  * done from any thread.
  *
  * Set the callbacks before calling `bind`.
  */
final class JvmUDPSocket extends UDPSocket {
  import JvmUDPSocket.*

  private val socket = DatagramSocket(null: java.net.SocketAddress)

  // resolving a host name is slow, and the same few peers are written to all the time
  private val resolved = ConcurrentHashMap[String, InetAddress]()

  def peer: Peer = Peer(socket.getLocalAddress.getHostAddress, socket.getLocalPort)

  /** Binds to the given port (0 to let the system choose, see [[peer]]) and starts receiving. */
  def bind(port: Int, address: Option[String] = None): Unit = {
    socket.bind(address.fold(InetSocketAddress(port))(InetSocketAddress(_, port)))

    val receiver = Thread(() => receiveLoop(), s"udp-receiver-${socket.getLocalPort}")
    receiver.setDaemon(true)
    receiver.start()

    _onListening()
  }

  private def receiveLoop(): Unit = {
    val buffer = new Array[Byte](MaxDatagramSize)
    val packet = DatagramPacket(buffer, buffer.length)
    try
      while !socket.isClosed do {
        packet.setLength(buffer.length)
        socket.receive(packet)
        val from = Peer(packet.getAddress.getHostAddress, packet.getPort)
        try
          _onMessage(java.util.Arrays.copyOfRange(buffer, packet.getOffset, packet.getLength), from)
        catch { case NonFatal(e) => e.printStackTrace() }
      }
    catch {
      case _: SocketException if socket.isClosed => () // we are the ones closing it
      case NonFatal(e) => e.printStackTrace()
    } finally _onClose()
  }

  def send(message: Array[Byte], port: Int, address: String): Unit =
    socket.send(
      DatagramPacket(
        message,
        message.length,
        resolved.computeIfAbsent(address, InetAddress.getByName(_)),
        port
      )
    )

  def close(): Unit = socket.close()
}

object JvmUDPSocket {

  /** The biggest payload of a UDP datagram over IPv4. */
  private val MaxDatagramSize = 65507
}
