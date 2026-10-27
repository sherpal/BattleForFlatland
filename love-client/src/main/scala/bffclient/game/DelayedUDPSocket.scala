package bffclient.game

import scala.scalajs.js

import communication.udp.{Peer, UDPSocket}

/** Delays everything going through a socket, in both directions, by `delayMs` (BFF_LAG_MS simulates
  * a round trip of twice that), and loses `lossPercent`% of the datagrams (BFF_LOSS_PERCENT), to try
  * the game with a bad network. As the delays are timers, they have the precision of a frame.
  */
final class DelayedUDPSocket(underlying: UDPSocket, delayMs: Int, lossPercent: Int = 0)
    extends UDPSocket {
  private def lost: Boolean = math.random() * 100 < lossPercent

  def peer: Peer = underlying.peer

  def bind(port: Int, address: Option[String]): Unit = underlying.bind(port, address)

  def send(message: Array[Byte], port: Int, address: String): Unit = {
    if !lost then js.timers.setTimeout(delayMs.toDouble)(underlying.send(message, port, address))
    ()
  }

  override def setMessageCallback(callback: (Array[Byte], Peer) => Unit): Unit = {
    _onMessage = callback
    underlying.setMessageCallback { (message, from) =>
      if !lost then js.timers.setTimeout(delayMs.toDouble)(callback(message, from))
      ()
    }
  }

  override def setCloseCallback(callback: () => Unit): Unit = {
    _onClose = callback
    underlying.setCloseCallback(callback)
  }

  override def setListeningCallback(callback: () => Unit): Unit = {
    _onListening = callback
    underlying.setListeningCallback(callback)
  }

  def close(): Unit = underlying.close()
}
