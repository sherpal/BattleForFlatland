package bffclient.game

import java.io.IOException

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future, Promise}

import bffclient.lua.LuaUDPSocket
import communication.BFFPicklers.*
import communication.udp.{Connection, Envelope, Peer, Rendezvous, Stun, TimersImpl, UDPSocket}
import communication.udp.Connection.Delivery
import models.bff.ingame.{GameUserCredentials, InGameWSProtocol}

/** The connection to a game server, through UDP (see `communication.udp`).
  *
  * It is opened by [[GameLink.open]]: we punch all the addresses of the game server, and register
  * at the rendezvous meanwhile, so that it can open its NAT to us (see [[Rendezvous]]), with our
  * public address if it sees us on a private network (see [[Stun]]). The first of its addresses
  * that answers is the one we say hello to (until it answers) with our credentials. Then we
  * synchronize our clock with its clock. Then messages can be sent, and the ones received are
  * collected until they are [[poll]]ed, so none is lost while the game is being set up. The
  * connection is deemed lost (see [[isOpen]]) when the server does not talk for a while.
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

  private var registered = false // at the rendezvous, with all it needs
  private val stun       = Stun.Lookup(socket, timers)

  /** The first address of the game server that answered our punches. */
  private var server = Option.empty[Peer]

  /** Once the game server accepted us. */
  private var connection = Option.empty[Connection[InGameWSProtocol]]

  socket.setMessageCallback(receive)

  private val credentials = GameUserCredentials(access.userName, access.gameId, access.secret)

  /** Whether a datagram from `from` comes from `address`. We can only tell for sure when it was
    * given as an IP address (the port is checked in any case).
    */
  private def isFrom(address: Peer, from: Peer): Boolean =
    from.port == address.port && (!isIpAddress(address.address) || from.address == address.address)

  /** The connection with the game server, if `from` is its address (created when it first answers
    * our hello, with Accepted, or Data, the datagrams being what they are).
    */
  private def connectionWith(from: Peer): Option[Connection[InGameWSProtocol]] =
    server.filter(isFrom(_, from)).map { server =>
      connection.getOrElse {
        val created = Connection[InGameWSProtocol](
          server,
          Envelope.DataSocket(socket),
          timers,
          inbox.enqueue(_),
          () => println("The game server stopped talking.")
        )
        connection = Some(created)
        created
      }
    }

  private def receive(datagram: Array[Byte], from: Peer): Unit =
    if Rendezvous.isRendezvous(datagram) then
      Rendezvous.parse(datagram).foreach {
        case Rendezvous.Packet.Registered(seenAs, publicAddress)
            if access.rendezvous.exists(isFrom(_, from)) && !registered =>
          registered = stun.answered(seenAs, publicAddress)
          if registered then
            println(
              s"Registered at the rendezvous, which sees us as $seenAs" +
                publicAddress.fold("")(address => s", and knows our public address $address")
            )
        case Rendezvous.Packet.Punch
            if server.isEmpty && access.addresses.exists(isFrom(_, from)) =>
          println(s"The game server answers at $from")
          server = Some(from)
          sendHello()
        case _ => ()
      }
    else if Stun.isStun(datagram) then stun.received(datagram)
    else
      Envelope.parse(datagram).foreach {
        case Envelope.Packet.Data(payload) => connectionWith(from).foreach(_.onMessage(payload))
        case Envelope.Packet.Accepted =>
          if connectionWith(from).isDefined then accepted.trySuccess(())
        case Envelope.Packet.Rejected(reason) =>
          if server.exists(isFrom(_, from)) then
            if accepted.isCompleted then forgotten = true
            else accepted.tryFailure(new IOException(rejection(reason)))
        case Envelope.Packet.Hello(_) => ()
      }

  private val hello    = Envelope.hello(credentials)
  private val punch    = Rendezvous.encode(Rendezvous.Packet.Punch)

  /** Punches every address of the game server until one answers, then says hello to that one. */
  private def sendHello(): Unit = server match {
    case Some(server) => socket.send(hello, server.port, server.address)
    case None =>
      access.addresses.foreach(address => socket.send(punch, address.port, address.address))
      if !registered then
        access.rendezvous.foreach(rendezvous =>
          socket.send(
            Rendezvous.encode(Rendezvous.Packet.PlayerRegister(credentials, stun.address)),
            rendezvous.port,
            rendezvous.address
          )
        )
  }

  private def start()(using ExecutionContext): Future[GameLink] = {
    sendHello()
    // Datagrams get lost, so we say it again until the server answers
    val helloTimer = timers.setInterval(HelloInterval)(sendHello())

    val clockSynchronized = Promise[Long]()
    val result = for
      _ <- timers.within("the game server does not answer", HandshakeTimeout)(accepted.future)
      _          = helloTimer.cancel()
      connection = this.connection.get
      _          = connection.connect()
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
    if isOpen then connection.foreach(_.send(message, delivery))

  /** The next message received, if any. */
  def poll(): Option[InGameWSProtocol] = Option.when(inbox.nonEmpty)(inbox.dequeue())

  def isOpen: Boolean = !closed && !forgotten && connection.exists(_.connected)

  /** The game server's clock (in milliseconds since the epoch, as `System.currentTimeMillis` on the
    * server), estimated.
    */
  def serverNow: Long = connection.fold(System.currentTimeMillis())(_.linkTime)

  /** Latency of the connection, there and back, in milliseconds. */
  def roundTrip: Long = connection.fold(0L)(2L * _.meanLatency)

  def close(): Unit = if !closed then {
    closed = true
    connection.foreach(_.disconnect())
    socket.close()
  }
}

object GameLink {
  private val HelloInterval     = 500
  // the game server may need a moment to open its NAT to us (see Rendezvous)
  private val HandshakeTimeout  = 15000
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
