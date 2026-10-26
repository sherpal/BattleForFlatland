package communication.udp

// see ConnectionJvmTest for why this is only tested on the JVM
final class EnvelopeJvmTest extends munit.FunSuite {

  test("a hello that cannot be decoded is ignored") {
    assertEquals(Envelope.parse(Array[Byte](1)), None)
    assertEquals(Envelope.parse(Array[Byte](1, 127, 127, 127, 127)), None)
    assertEquals(Envelope.parse(Array[Byte](1, 5, 'a', 'b')), None)
  }
}
