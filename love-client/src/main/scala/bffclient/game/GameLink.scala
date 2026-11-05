package bffclient.game

import java.io.IOException

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future, Promise}

import bffclient.lua.LuaUDPSocket
import communication.BFFPicklers.*
import scala.util.Random

import communication.udp.{Connection, Envelope, Hmac, Peer, Rendezvous, Stun, TimersImpl, UDPSocket}
import communication.udp.Connection.Delivery
import models.bff.ingame.InGameWSProtocol
import services.i18n.I18n

/** The connection to a game server, through UDP (see `communication.udp`).
  *
  * It is opened by [[GameLink.open]]: we punch all the addresses of the game server, and register
  * at the rendezvous meanwhile, so that it can open its NAT to us (see [[Rendezvous]]), with our
  * public address if it sees us on a private network (see [[Stun]]). The first of its addresses
  * that answers is the one we say hello to, and prove who we are to (with our secret, which never
  * travels, see [[Envelope]]), until it accepts us, proving that it is the game server. Then we
  * synchronize our clock with its clock. Then messages can be sent, and the ones received are
  * collected until they are [[poll]]ed, so none is lost while the game is being set up. The
  * connection is deemed lost (see [[isOpen]]) when the server does not talk for a while.
  */
final class GameLink private (access: GameAccess, i18n: I18n, lag: Double, lossPercent: Int) {
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

  private val secret      = Hmac(access.secret)
  private val counter     = Rendezvous.Counter()
  private val clientNonce = Envelope.Nonce.fresh(() => Random.nextInt())

  /** Once the game server challenged our hello: what we prove ourselves with, and the keys of the
    * session.
    */
  private var proof = Option.empty[(Envelope.Packet.Proof, Envelope.SessionKeys)]

  /** Whether a datagram from `from` comes from `address`. We can only tell for sure when it was
    * given as an IP address (the port is checked in any case).
    */
  private def isFrom(address: Peer, from: Peer): Boolean =
    from.port == address.port && (!isIpAddress(address.address) || from.address == address.address)

  /** The connection with the game server, if `from` is its address and it accepted our proof
    * (created when it first answers it, with Accepted, or Data, the datagrams being what they are).
    */
  private def connectionWith(
      from: Peer
  ): Option[(Connection[InGameWSProtocol], Envelope.SessionKeys)] =
    for
      server    <- server.filter(isFrom(_, from))
      (_, keys) <- proof
    yield connection.getOrElse {
      val created = Connection[InGameWSProtocol](
        server,
        Envelope.DataSocket(socket, keys),
        timers,
        inbox.enqueue(_),
        () => println("The game server stopped talking.")
      )
      connection = Some(created)
      created
    } -> keys

  private def receive(datagram: Array[Byte], from: Peer): Unit =
    if Rendezvous.isRendezvous(datagram) then
      Rendezvous.parseSignedWith(datagram, secret).foreach {
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
        case data: Envelope.Packet.Data =>
          for
            (_, keys) <- proof if server.exists(isFrom(_, from))
            payload   <- keys.open(data)
            (link, _) <- connectionWith(from)
          do link.onMessage(payload)
        case Envelope.Packet.Challenge(nonce, serverNonce)
            if nonce == clientNonce && proof.isEmpty && server.exists(isFrom(_, from)) =>
          val nonces = Envelope.Packet.Challenge(nonce, serverNonce)
          proof = Some(
            Envelope.proof(access.userName, access.gameId, nonces, secret) ->
              Envelope.SessionKeys(secret, nonces, towardsServer = true)
          )
          sendHello()
        case packet: Envelope.Packet.Accepted =>
          if proof.exists(_._2.isAccepted(packet)) && connectionWith(from).isDefined then
            accepted.trySuccess(())
        case Envelope.Packet.Rejected(reason) =>
          if server.exists(isFrom(_, from)) then
            if accepted.isCompleted then forgotten = true
            else accepted.tryFailure(new IOException(rejection(reason, i18n)))
        case _ => ()
      }

  private val hello = Envelope.encode(
    Envelope.Packet.Hello(access.userName, access.gameId, clientNonce)
  )

  /** Punches every address of the game server until one answers, then says hello to that one, and
    * proves who we are once challenged.
    */
  private def sendHello(): Unit = server match {
    case Some(server) =>
      val datagram = proof.fold(hello)((proof, _) => Envelope.encode(proof))
      socket.send(datagram, server.port, server.address)
    case None =>
      access.addresses.foreach(address =>
        socket.send(Rendezvous.punch, address.port, address.address)
      )
      if !registered then
        access.rendezvous.foreach(rendezvous =>
          socket.send(
            Rendezvous.encode(
              Rendezvous.Packet.PlayerRegister(
                access.userName,
                access.gameId,
                counter.next(),
                stun.address
              ),
              secret
            ),
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
      _ <- timers.within(i18n.gameServerDoesNotAnswer, HandshakeTimeout)(accepted.future)
      _          = helloTimer.cancel()
      connection = this.connection.get
      _          = connection.connect()
      _ = connection.computeLinkTime(
        ClockSyncInterval,
        ClockSyncSamples,
        delta => clockSynchronized.trySuccess(delta)
      )
      delta <- timers.within(i18n.cannotSynchronizeClock, ClockSyncTimeout)(
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
  private val HelloInterval = 500
  // the game server may need a moment to open its NAT to us (see Rendezvous)
  private val HandshakeTimeout  = 15000
  private val ClockSyncInterval = 50
  private val ClockSyncSamples  = 10
  private val ClockSyncTimeout  = 15000

  /** Connects to the game server, and completes when everything is ready to play. `lag` (a round
    * trip, in seconds) and `lossPercent` simulate a bad network.
    */
  /** The errors are in the language of `i18n`. */
  def open(access: GameAccess, i18n: I18n, lag: Double = 0, lossPercent: Int = 0)(using
      ExecutionContext
  ): Future[GameLink] =
    new GameLink(access, i18n, lag, lossPercent).start()

  private def isIpAddress(host: String): Boolean = host.matches("""\d{1,3}(\.\d{1,3}){3}""")

  private def rejection(reason: Byte, i18n: I18n): String =
    if reason == Envelope.RejectReason.NotReady then i18n.gameServerNotReady
    else if reason == Envelope.RejectReason.Unauthorized then i18n.gameServerRejectsCredentials
    else i18n.gameServerDoesNotKnowUs

}
