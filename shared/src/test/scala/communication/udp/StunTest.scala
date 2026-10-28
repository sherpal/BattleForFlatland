package communication.udp

import models.bff.ingame.GameUserCredentials

final class StunTest extends munit.FunSuite {

  private def bytes(hex: String): Array[Byte] =
    hex.split(" ").filter(_.nonEmpty).map(Integer.parseInt(_, 16).toByte)

  private val transactionId = bytes("b7 e7 a7 01 bc 34 d6 86 fa 87 df ae")

  /** The IPv4 response of RFC 5769 (2.2), without its integrity and fingerprint attributes. */
  private val response = bytes(
    "01 01 00 1c 21 12 a4 42 b7 e7 a7 01 bc 34 d6 86 fa 87 df ae " +
      "80 22 00 0b 74 65 73 74 20 76 65 63 74 6f 72 20 " + // software, padded
      "00 20 00 08 00 01 a1 47 e1 12 a6 43" // XOR-mapped address
  )

  test("the request is a binding request with our transaction id") {
    val request = Stun.request(transactionId)
    assertEquals(request.take(8).toSeq, bytes("00 01 00 00 21 12 a4 42").toSeq)
    assertEquals(request.drop(8).toSeq, transactionId.toSeq)
    assert(Stun.isStun(request))
  }

  test("the address is read from the response") {
    assert(Stun.isStun(response))
    assertEquals(Stun.parse(response, transactionId), Some(Peer("192.0.2.1", 32853)))
  }

  test("old servers only give the mapped address") {
    val old = bytes(
      "01 01 00 0c 21 12 a4 42 b7 e7 a7 01 bc 34 d6 86 fa 87 df ae " +
        "00 01 00 08 00 01 1f 90 51 0c 22 38"
    )
    assertEquals(Stun.parse(old, transactionId), Some(Peer("81.12.34.56", 8080)))
  }

  test("the answers to other requests are ignored") {
    assertEquals(Stun.parse(response, Stun.newTransactionId()), None)
    assertEquals(Stun.parse(Stun.request(transactionId), transactionId), None)
    assertEquals(Stun.parse(response.dropRight(4), transactionId), None)
  }

  test("our other datagrams are not taken for STUN") {
    val credentials = GameUserCredentials("some user", "game-id", "s3cr3t")
    for datagram <- List(
        Envelope.hello(credentials),
        Envelope.accepted,
        Envelope.data(Array.fill[Byte](40)(0)),
        Rendezvous.encode(Rendezvous.Packet.PlayerRegister(credentials, None)),
        Rendezvous.encode(Rendezvous.Packet.Punch)
      )
    do assert(!Stun.isStun(datagram))
  }

  test("private networks") {
    for address <- List("127.0.0.1", "10.1.2.3", "172.16.0.1", "172.31.255.1", "192.168.1.12",
        "169.254.3.4", "100.64.0.1", "100.127.1.1", "localhost", "::1")
    do assert(Peer(address, 1).onPrivateNetwork, address)
    for address <- List("81.12.34.56", "172.32.0.1", "100.128.0.1", "8.8.8.8", "192.169.0.1")
    do assert(!Peer(address, 1).onPrivateNetwork, address)
  }
}
