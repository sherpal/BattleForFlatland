package bffclient.game

import java.io.IOException

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future, Promise}

import bffclient.lua.LuaUDPSocket
import communication.BFFPicklers.*
import communication.udp.{Connection, Envelope, Peer, TimersImpl, UDPSocket}
import communication.udp.Connection.Delivery
import models.bff.ingame.{GameUserCredentials, InGameWSProtocol}

/** The connection to a game server, through UDP (see `communication.udp`).
  *
  * It is opened by [[GameLink.open]]: we say hello to the game server (until it answers) with our
  * credentials, then synchronize our clock with its clock. Then messages can be sent, and the ones
  * received are collected until they are [[poll]]ed, so none is lost while the game is being set
  * up. The connection is deemed lost (see [[isOpen]]) when the server does not talk for a while.
  */
final class GameLink private (access: GameAccess, lag: Double, lossPercent: Int) {
  import GameLink.*

  private val timers = TimersImpl()
  private val socket: UDPSocket = {
    val raw = LuaUDPSocket()
    // `lag` is a round trip, in seconds
    if lag > 0 || lossPercent > 0 then DelayedUDPSocket(raw, (lag * 500).toInt, lossPercent)
    else raw
  }
  private val inbox     = mutable.Queue.empty[InGameWSProtocol]
  private val accepted  = Promise[Unit]()
  private var closed    = false
  private var forgotten = false // the server answered that it does not know us (anymore)

  private val connection = Connection[InGameWSProtocol](
    Peer(access.host, access.port),
    Envelope.DataSocket(socket),
    timers,
    inbox.enqueue(_),
    () => println("The game server stopped talking.")
  )

  socket.setMessageCallback(receive)

  /** Only datagrams from the game server count. We can only tell for sure when it was given as an
    * IP address (the port is checked in any case).
    */
  private def fromServer(from: Peer): Boolean =
    from.port == access.port && (!isIpAddress(access.host) || from.address == access.host)

  private def receive(datagram: Array[Byte], from: Peer): Unit =
    if fromServer(from) then
      Envelope.parse(datagram).foreach {
        // Data can come before Accepted, the datagrams being what they are
        case Envelope.Packet.Data(payload) => connection.onMessage(payload)
        case Envelope.Packet.Accepted => accepted.trySuccess(())
        case Envelope.Packet.Rejected(reason) =>
          if accepted.isCompleted then forgotten = true
          else accepted.tryFailure(new IOException(rejection(reason)))
        case Envelope.Packet.Hello(_) => ()
      }

  private def start()(using ExecutionContext): Future[GameLink] = {
    val hello =
      Envelope.hello(GameUserCredentials(access.userName, access.gameId, access.secret))
    def sendHello(): Unit = socket.send(hello, access.port, access.host)
    sendHello()
    // Datagrams get lost, so we say it again until the server answers
    val helloTimer = timers.setInterval(HelloInterval)(sendHello())

    val clockSynchronized = Promise[Long]()
    val result = for
      _ <- timers.within("the game server does not answer", HandshakeTimeout)(accepted.future)
      _ = helloTimer.cancel()
      _ = connection.connect()
      _ = connection.computeLinkTime(
        ClockSyncInterval,
        ClockSyncSamples,
        delta => clockSynchronized.trySuccess(delta)
      )
      delta <- timers.within("cannot synchronize the clock with the game server", ClockSyncTimeout)(
        clockSynchronized.future
      )
    yield {
      println(s"Clock synchronized with the game server: it is $delta ms ahead of us")
      this
    }
    result.transform(
      identity,
      error => {
        helloTimer.cancel()
        close()
        error
      }
    )
  }

  /** Sends a message to the game server. The default is for what has to arrive, in order. */
  def send(message: InGameWSProtocol, delivery: Delivery = Delivery.OrderedReliable): Unit =
    if isOpen then connection.send(message, delivery)

  /** The next message received, if any. */
  def poll(): Option[InGameWSProtocol] = Option.when(inbox.nonEmpty)(inbox.dequeue())

  def isOpen: Boolean = !closed && !forgotten && connection.connected

  /** The game server's clock (in milliseconds since the epoch, as `System.currentTimeMillis` on the
    * server), estimated.
    */
  def serverNow: Long = connection.linkTime

  /** Latency of the connection, there and back, in milliseconds. */
  def roundTrip: Long = 2L * connection.meanLatency

  def close(): Unit = if !closed then {
    closed = true
    connection.disconnect()
    socket.close()
  }
}

object GameLink {
  private val HelloInterval     = 500
  private val HandshakeTimeout  = 10000
  private val ClockSyncInterval = 50
  private val ClockSyncSamples  = 10
  private val ClockSyncTimeout  = 15000

  /** Connects to the game server, and completes when everything is ready to play. `lag` (a round
    * trip, in seconds) and `lossPercent` simulate a bad network.
    */
  def open(access: GameAccess, lag: Double = 0, lossPercent: Int = 0)(using
      ExecutionContext
  ): Future[GameLink] =
    new GameLink(access, lag, lossPercent).start()

  private def isIpAddress(host: String): Boolean = host.matches("""\d{1,3}(\.\d{1,3}){3}""")

  private def rejection(reason: Byte): String =
    if reason == Envelope.RejectReason.NotReady then "the game server is not ready"
    else if reason == Envelope.RejectReason.Unauthorized then
      "the game server does not accept our credentials"
    else "the game server does not know us"

}
