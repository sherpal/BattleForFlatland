package bffclient.lua

import java.io.IOException
import scala.scalajs.js
import scala.util.control.NonFatal
import communication.udp.{Peer, UDPSocket}

import scala.annotation.tailrec

/** A [[UDPSocket]] on top of LuaSocket (see src/main/lua/udp.lua).
  *
  * There are no threads in LÖVE: the datagrams received are collected every frame, at the start of
  * `love.update` (like the timers of [[communication.udp.TimersImpl]], which wait for the same
  * moment), and handed to the message callback, so a message takes up to a frame to be seen. At
  * most [[LuaUDPSocket.MaxDatagramsPerFrame]] are handled per frame, the next ones wait for the
  * next frame.
  *
  * Sending a datagram never fails: if it cannot be sent, it is as if it was lost, which is the
  * nature of UDP. The first failure is printed.
  *
  * Host names are resolved when first used, which blocks (once per name): prefer IP addresses in
  * frames that matter. The sockets are IPv4 only.
  */
final class LuaUDPSocket extends UDPSocket {
  import LuaUDPSocket.*

  private val handle: UdpHandle = UdpModule.create()

  private var polling: Option[js.timers.SetIntervalHandle] = None
  private var closed                                       = false
  private var sendFailureReported                          = false

  /** The local address: only known once bound, or after something was sent. */
  def peer: Peer = {
    val name = handle.getsockname()
    Peer(name.ip, name.port)
  }

  /** Binds to the given port (0 to let the system choose, see [[peer]]) and starts receiving. A
    * client can skip it: its first `send` gets it a port, and it starts receiving then.
    *
    * @throws java.io.IOException
    *   if the address cannot be bound
    */
  def bind(port: Int, address: Option[String] = None): Unit = {
    val result = handle.bind(port, address.orNull)
    if !result.ok then throw new IOException(s"Cannot bind UDP port $port: ${result.error}")
    startPolling()
    _onListening()
  }

  def send(message: Array[Byte], port: Int, address: String): Unit = {
    // Receiving only starts when bound, but a client that never does must still receive the answers
    if polling.isEmpty && !closed then startPolling()
    val result = handle.send(ScalaLove.bytesToString(message), address, port)
    if !result.ok && !sendFailureReported then {
      sendFailureReported = true
      println(
        s"UDP: cannot send to $address:$port: ${result.error} (further failures are not reported)"
      )
    }
  }

  def close(): Unit = if !closed then {
    closed = true
    polling.foreach(js.timers.clearInterval)
    polling = None
    handle.close()
    _onClose()
  }

  private def startPolling(): Unit =
    if polling.isEmpty then polling = Some(js.timers.setInterval(1)(receive()))

  @tailrec private def receive(remaining: Int = MaxDatagramsPerFrame): Unit =
    handle.poll().toOption match {
      case None => ()
      case Some(datagram) =>
        try _onMessage(ScalaLove.stringToBytes(datagram.data), Peer(datagram.ip, datagram.port))
        catch {
          case NonFatal(e) => e.printStackTrace()
        }
        receive(remaining - 1)
    }
}

object LuaUDPSocket {

  /** Bounds the time spent receiving in a frame, e.g. when the game was frozen for a while. */
  private val MaxDatagramsPerFrame = 512
}
