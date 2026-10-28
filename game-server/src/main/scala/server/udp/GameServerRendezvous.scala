package server.udp

import communication.udp.*
import communication.udp.Rendezvous.Packet

import scala.concurrent.{Future, Promise}

/** The game server's side of the rendezvous (see [[Rendezvous]]): says [[Packet.ServerRegister]] to
  * the menu server every second, and sends [[Packet.Punch]] to the players it answers with, unless
  * they already have a session (which opens our NAT to them). It answers the players' punches.
  *
  * The socket must be the one of the players' sessions (the datagrams of the rendezvous it receives
  * are given to [[received]], see [[Rendezvous.SharedSocket]]).
  */
final class GameServerRendezvous(
    socket: UDPSocket,
    timers: Timers,
    menuServer: Peer,
    gameId: String,
    gameSecret: String,
    isConnected: String => Boolean
) {
  private val register   = Rendezvous.encode(Packet.ServerRegister(gameId, gameSecret))
  private val punch      = Rendezvous.encode(Packet.Punch)
  private val registered = Promise[Unit]()

  /** Completes when the menu server answered for the first time. */
  def isRegistered: Future[Unit] = registered.future

  def start(): Unit = {
    sendRegister()
    timers.setInterval(1000)(sendRegister())
  }

  private def sendRegister(): Unit =
    try socket.send(register, menuServer.port, menuServer.address)
    catch {
      // e.g., the network is down for a moment: we will say it again
      case e: java.io.IOException => println(s"Cannot reach the rendezvous: ${e.getMessage}")
    }

  def received(packet: Packet, from: Peer): Unit = packet match {
    case Packet.Players(players) if from.port == menuServer.port =>
      if registered.trySuccess(()) then println(s"Registered at the rendezvous ($menuServer)")
      for (userName, peer) <- players if !isConnected(userName) do
        socket.send(punch, peer.port, peer.address)
    case Packet.Punch => socket.send(punch, from.port, from.address)
    case _ => ()
  }
}
