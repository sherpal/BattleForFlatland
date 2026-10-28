package communication.udp

import communication.udp.Rendezvous.Packet
import models.bff.ingame.GameUserCredentials

final class RendezvousTest extends munit.FunSuite {

  private val credentials = GameUserCredentials("some user", "game-id", "s3cr3t-é")

  test("every packet goes through") {
    for packet <- List(
        Packet.ServerRegister("game-id", "game secret"),
        Packet.Players(Vector("alice" -> Peer("81.12.34.56", 40000), "bob" -> Peer("::1", 1))),
        Packet.Players(Vector.empty),
        Packet.PlayerRegister(credentials),
        Packet.Registered(Peer("81.12.34.56", 40000)),
        Packet.Punch
      )
    do {
      val datagram = Rendezvous.encode(packet)
      assert(Rendezvous.isRendezvous(datagram), packet)
      assertEquals(Rendezvous.parse(datagram), Some(packet))
    }
  }

  test("rendezvous and envelope datagrams are told apart") {
    for datagram <- List(
        Envelope.hello(credentials),
        Envelope.accepted,
        Envelope.rejected(Envelope.RejectReason.NoSession),
        Envelope.data(Array[Byte](16, 17, 18))
      )
    do {
      assert(!Rendezvous.isRendezvous(datagram))
      assertEquals(Rendezvous.parse(datagram), None)
    }
    assert(!Rendezvous.isRendezvous(Array.emptyByteArray))
  }

  test("garbage is ignored") {
    assertEquals(Rendezvous.parse(Array[Byte](16, 1, 2)), None)
    assertEquals(Rendezvous.parse(Array.fill[Byte](Rendezvous.MaxSize + 1)(19)), None)
  }
}
