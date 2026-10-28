package communication.udp

final case class Peer(address: String, port: Int) {

  /** Whether this is an address of a private network (or the loopback), which only the computers
    * of that network can reach: the rendezvous then does not see the public address of the peer,
    * which has to find it out by itself (see [[Stun]]).
    */
  def onPrivateNetwork: Boolean = Peer.onPrivateNetwork(address)
}

object Peer {

  private val Ipv4 = """(\d{1,3})\.(\d{1,3})\.\d{1,3}\.\d{1,3}""".r

  def onPrivateNetwork(address: String): Boolean = address match {
    case Ipv4(first, second) =>
      (first.toInt, second.toInt) match {
        case (127, _) | (10, _) | (192, 168) | (169, 254) => true
        case (172, b) => b >= 16 && b < 32
        case (100, b) => b >= 64 && b < 128 // shared by the carriers' NATs, and VPNs
        case _ => false
      }
    case "localhost" | "::1" | "0:0:0:0:0:0:0:1" => true
    case _ => false
  }
}
