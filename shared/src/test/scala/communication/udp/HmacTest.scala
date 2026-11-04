package communication.udp

final class HmacTest extends munit.FunSuite {

  private def hex(bytes: Array[Byte]): String = bytes.map(b => f"${b & 0xff}%02x").mkString

  private def unhex(s: String): Array[Byte] =
    s.grouped(2).map(Integer.parseInt(_, 16).toByte).toArray

  test("SHA-256, FIPS 180-4's examples") {
    for digest <- List(Sha256.digest, PortableSha256.digest) do {
      assertEquals(
        hex(digest(Array.emptyByteArray)),
        "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
      )
      assertEquals(
        hex(digest(Hmac.utf8("abc"))),
        "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
      )
      assertEquals(
        hex(digest(Hmac.utf8("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq"))),
        "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1"
      )
    }
  }

  test("the portable SHA-256 is the platform's, around the sizes of the blocks") {
    for length <- List(1, 55, 56, 63, 64, 65, 119, 120, 1000) do {
      val data = Array.tabulate[Byte](length)(i => (i * 13 + 7).toByte)
      assertEquals(hex(PortableSha256.digest(data)), hex(Sha256.digest(data)), length)
    }
  }

  test("HMAC-SHA256, RFC 4231's test cases") {
    assertEquals(
      hex(Hmac(Array.fill[Byte](20)(0x0b))(Hmac.utf8("Hi There"))),
      "b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7"
    )
    assertEquals(
      hex(Hmac("Jefe")(Hmac.utf8("what do ya want for nothing?"))),
      "5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843"
    )
    // a key bigger than a block
    assertEquals(
      hex(
        Hmac(Array.fill[Byte](131)(0xaa.toByte))(
          Hmac.utf8("Test Using Larger Than Block-Size Key - Hash Key First")
        )
      ),
      "60e431591ee0b67f0d8a26aacbf5b77f8e0bc6213728c5140546040f0ee37f54"
    )
  }

  test("the parts are concatenated") {
    val key = Hmac("key")
    assertEquals(hex(key(unhex("0102"), unhex("03"))), hex(key(unhex("010203"))))
  }

  test("tags are verified") {
    val key = Hmac("key")
    val tag = key.tag(Hmac.utf8("message"))
    assertEquals(tag.length, Hmac.TagSize)
    assert(key.verify(tag, Hmac.utf8("message")))
    assert(!key.verify(tag, Hmac.utf8("massage")))
    assert(!Hmac("other").verify(tag, Hmac.utf8("message")))
    assert(!key.verify(tag.take(8), Hmac.utf8("message")))
  }

  test("longs") {
    for n <- List(0L, 1L, 255L, 256L, -1L, Long.MaxValue, Long.MinValue, 1730000000000L) do
      assertEquals(Hmac.readLong(Hmac.longBytes(n), 0), n)
  }
}
