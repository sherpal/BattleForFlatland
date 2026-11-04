package server.udp

import communication.udp.*
import communication.udp.Rendezvous.Packet

import scala.concurrent.{Future, Promise}

/** The game server's side of the rendezvous (see [[Rendezvous]]): says [[Packet.ServerRegister]] to
  * the menu server every second, and sends [[Packet.Punch]] to the players it answers with, unless
  * they already have a session (which opens our NAT to them). It answers the players' punches.
  *
  * When the menu server sees us on a private network, we ask a STUN server our public address, and
  * tell it to the menu server (see [[Stun]]).
  *
  * The registers are signed with the secret of the game, and so are the answers of the menu server
  * (see [[Rendezvous]]): the others are ignored.
  *
  * The socket must be the one of the players' sessions (the datagrams of the rendezvous it receives
  * are given to [[received]], and the ones of STUN to [[receivedStun]], see
  * [[Rendezvous.SharedSocket]]).
  */
final class GameServerRendezvous(
    socket: UDPSocket,
    timers: Timers,
    menuServer: Peer,
    gameId: String,
    gameSecret: String,
    isConnected: String => Boolean
) {
  private val punch      = Rendezvous.punch
  private val key        = Hmac(gameSecret)
  private val counter    = Rendezvous.Counter()
  private val registered = Promise[Unit]()
  private val stun       = Stun.Lookup(socket, timers, onFound = _ => sendRegister())

  /** Completes when the menu server knows all it can about us: where it sees us from, and our
    * public address (if it needs it, and we found it).
    */
  def isRegistered: Future[Unit] = registered.future

  def start(): Unit = {
    sendRegister()
    timers.setInterval(1000)(sendRegister())
  }

  private def sendRegister(): Unit =
    try
      socket.send(
        Rendezvous.encode(Packet.ServerRegister(gameId, counter.next(), stun.address), key),
        menuServer.port,
        menuServer.address
      )
    catch {
      // e.g., the network is down for a moment: we will say it again
      case e: java.io.IOException => println(s"Cannot reach the rendezvous: ${e.getMessage}")
    }

  def received(received: Rendezvous.Received, from: Peer): Unit = received.packet match {
    case Packet.Players(you, players) if from.port == menuServer.port && received.signedWith(key) =>
      if stun.answered(you.seenAs, you.publicAddress) && registered.trySuccess(()) then
        println(
          s"Registered at the rendezvous ($menuServer), which sees us as ${you.seenAs}" +
            you.publicAddress.fold("")(address => s", and knows our public address $address")
        )
      for (userName, peer) <- players if !isConnected(userName) do
        socket.send(punch, peer.port, peer.address)
    case Packet.Punch => socket.send(punch, from.port, from.address)
    case _ => ()
  }

  def receivedStun(datagram: Array[Byte]): Unit = stun.received(datagram)
}
